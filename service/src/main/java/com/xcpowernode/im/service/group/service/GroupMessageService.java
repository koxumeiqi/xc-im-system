package com.xcpowernode.im.service.group.service;


import cn.hutool.core.util.ObjectUtil;
import com.xcpower.codec.pack.message.ChatMessageAck;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.command.GroupEventCommand;
import com.xcpower.im.enums.command.MessageCommand;
import com.xcpower.im.model.ClientInfo;
import com.xcpower.im.model.SyncReq;
import com.xcpower.im.model.message.GroupChatMessageContent;
import com.xcpower.im.model.message.OfflineMessageContent;
import com.xcpowernode.im.service.group.model.req.SendGroupMessageReq;
import com.xcpowernode.im.service.message.model.resp.SendMessageResp;
import com.xcpowernode.im.service.message.service.CheckSendMessageService;
import com.xcpowernode.im.service.message.service.MessageStoreService;
import com.xcpowernode.im.service.seq.RedisSeq;
import com.xcpowernode.im.service.utils.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class GroupMessageService {


    @Autowired
    CheckSendMessageService checkSendMessageService;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    @Autowired
    MessageStoreService messageStoreService;

    @Autowired
    RedisSeq redisSeq;

    private final ThreadPoolExecutor threadPoolExecutor;

    {
        AtomicInteger num = new AtomicInteger(0);
        threadPoolExecutor = new ThreadPoolExecutor(
                6, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setDaemon(true);
                        thread.setName("message-group-thread-" + num.getAndIncrement());
                        return thread;
                    }
                }
        );
    }

    public void process(GroupChatMessageContent groupChatMessageContent) {

        String fromId = groupChatMessageContent.getFromId();
        String toId = groupChatMessageContent.getToId();
        Integer appId = groupChatMessageContent.getAppId();

        // 从缓存中取，如果取着了说明消息重复发了，直接分发不存储，保持幂等性
        GroupChatMessageContent cache = messageStoreService.getMessageFromMessageIdCache(appId,
                groupChatMessageContent.getMessageId(), GroupChatMessageContent.class);
        if (!ObjectUtils.isEmpty(cache)) {
            threadPoolExecutor.execute(() -> {
                // 1. 回 ack 给自己
                ack(groupChatMessageContent, ResponseVO.successResponse());
                // 2. 发消息给同步在线端
                syncToSender(groupChatMessageContent, new ClientInfo(groupChatMessageContent.getAppId(), groupChatMessageContent.getClientType(), groupChatMessageContent.getImei()));
                // 3. 发消息给对方在线端
                dispatchMessage(groupChatMessageContent);

            });
            return;
        }


        // 前置校验
        // 这个用户是否被禁言、是否被禁用
        // 发送方和接收方是否是好友
/*        ResponseVO responseVO = imServerPermissionCheck(fromId, groupChatMessageContent.getGroupId(),
                groupChatMessageContent.getAppId());
        if (responseVO.isOk()) {*/

        threadPoolExecutor.execute(() -> {
            long seq = redisSeq.doGetSeq(groupChatMessageContent.getAppId() + ":" +
                    Constants.SeqConstants.GroupMessage + ":"
                    + groupChatMessageContent.getGroupId());
            groupChatMessageContent.setMessageSequence(seq);

            messageStoreService.storeGroupMessage(groupChatMessageContent);

            // 存储离线消息
            List<String> groupMemberId = imGroupMemberService.getGroupMemberId(groupChatMessageContent.getGroupId(),
                    groupChatMessageContent.getAppId());
            groupChatMessageContent.setMemberId(groupMemberId);
            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(groupChatMessageContent, offlineMessageContent);
            offlineMessageContent.setToId(groupChatMessageContent.getGroupId());
            messageStoreService.storeOfflineGroupMessage(offlineMessageContent, groupChatMessageContent.getMemberId());

            // 1. 回 ack 给自己
            ack(groupChatMessageContent, ResponseVO.successResponse());
            // 2. 发消息给同步在线端
            syncToSender(groupChatMessageContent, new ClientInfo(groupChatMessageContent.getAppId(), groupChatMessageContent.getClientType(), groupChatMessageContent.getImei()));
            // 3. 发消息给对方在线端
            dispatchMessage(groupChatMessageContent);

            messageStoreService.setMessageFromMessageIdCache(groupChatMessageContent.getAppId(),
                    groupChatMessageContent.getMessageId(),
                    groupChatMessageContent);
        });

        /*} else {
            // 告诉客户端失败了
            // ack
            ack(groupChatMessageContent, responseVO);
        }*/

    }

    private void ack(GroupChatMessageContent groupChatMessageContent, ResponseVO responseVO) {

        ChatMessageAck chatMessageAck = new ChatMessageAck(groupChatMessageContent.getMessageId());
        responseVO.setData(groupChatMessageContent.getMessageBody());
        // 发消息 同步在线端
        messageProducer.sendToUser(groupChatMessageContent.getFromId(), GroupEventCommand.GROUP_MSG_ACK,
                responseVO,
                new ClientInfo(groupChatMessageContent.getAppId(), groupChatMessageContent.getClientType(), groupChatMessageContent.getImei()));

    }

    /**
     * 转发消息给对方
     *
     * @param groupChatMessageContent
     */
    private void dispatchMessage(GroupChatMessageContent groupChatMessageContent) {

        List<String> groupMemberId = imGroupMemberService.getGroupMemberId(groupChatMessageContent.getGroupId(),
                groupChatMessageContent.getAppId());

        for (String memberId : groupMemberId) {
            if (!memberId.equals(groupChatMessageContent.getFromId())) {
                messageProducer.sendToUser(memberId,
                        GroupEventCommand.MSG_GROUP,
                        groupChatMessageContent,
                        groupChatMessageContent.getAppId());
            }
        }
    }

    /**
     * 同步消息到自身其他端
     *
     * @param groupChatMessageContent
     * @param clientInfo
     */
    private void syncToSender(GroupChatMessageContent groupChatMessageContent, ClientInfo clientInfo) {
        messageProducer.sendToUserExceptClient(groupChatMessageContent.getFromId(),
                GroupEventCommand.MSG_GROUP, groupChatMessageContent,
                clientInfo);
    }

    public ResponseVO imServerPermissionCheck(String fromId,
                                              String groupId,
                                              Integer appId) {

        ResponseVO responseVO = checkSendMessageService.checkGroupMessage(fromId,
                groupId,
                appId
        );

        return responseVO;
    }

    public SendMessageResp send(SendGroupMessageReq req) {

        SendMessageResp sendMessageResp = new SendMessageResp();
        GroupChatMessageContent message = new GroupChatMessageContent();
        BeanUtils.copyProperties(req, message);

        messageStoreService.storeGroupMessage(message);

        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());
        //2.发消息给同步在线端
        syncToSender(message, message);
        //3.发消息给对方在线端
        dispatchMessage(message);

        return sendMessageResp;

    }

}
