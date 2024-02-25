package com.xcpowernode.im.service.message.service;


import com.xcpower.codec.pack.message.ChatMessageAck;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.ConversationTypeEnum;
import com.xcpower.im.enums.command.MessageCommand;
import com.xcpower.im.model.ClientInfo;
import com.xcpower.im.model.message.MessageContent;
import com.xcpower.im.model.message.MessageRecieveServerAckPack;
import com.xcpower.im.model.message.OfflineMessageContent;
import com.xcpowernode.im.service.message.model.req.SendMessageReq;
import com.xcpowernode.im.service.message.model.resp.SendMessageResp;
import com.xcpowernode.im.service.seq.RedisSeq;
import com.xcpowernode.im.service.utils.ConversationIdGenerate;
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
public class P2PMessageService {

    @Autowired
    CheckSendMessageService checkSendMessageService;

    @Autowired
    MessageProducer messageProducer;

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
                        thread.setName("message-process-thread-" + num.getAndIncrement());
                        thread.setDaemon(true);
                        return thread;
                    }
                }
        );
    }

    // 接收消息处理
    public void process(MessageContent messageContent) {

        /*String fromId = messageContent.getFromId();
        String toId = messageContent.getToId();
        Integer appId = messageContent.getAppId();*/

        MessageContent cache = messageStoreService.getMessageFromMessageIdCache(messageContent.getAppId(),
                messageContent.getMessageId(),
                MessageContent.class);
        // 如果消息已经被存储过了，那直接进行分发即可
        // 即不需要再次进行存储
        if (!ObjectUtils.isEmpty(cache)) {
            threadPoolExecutor.execute(() -> {
                // 1. 回 ack 给自己
                ack(messageContent, ResponseVO.successResponse());
                // 2. 发消息给同步在线端
                syncToSender(cache, new ClientInfo(cache.getAppId(), cache.getClientType(), cache.getImei()));
                // 3. 发消息给对方在线端
                List<ClientInfo> clientInfos = dispatchMessage(cache);
                if (clientInfos.isEmpty()) {
                    revicerAck(cache);
                }
            });
            return;
        }

        // 获取消息序列号 Seq，准备返回给前端，让前端处理消息的有序性
        long seq = redisSeq.doGetSeq(
                messageContent.getAppId() + ":"
                        + Constants.SeqConstants.Message + ":" + ConversationIdGenerate.generateP2PId(
                        messageContent.getFromId(), messageContent.getToId()
                )
        );
        messageContent.setMessageSequence(seq);

        // 前置校验
        // 这个用户是否被禁言、是否被禁用
        // 发送方和接收方是否是好友
        // ResponseVO responseVO = imServerPermissionCheck(fromId, toId, messageContent.getAppId());
        // if (responseVO.isOk()) {
        // 使用线程池处理校验后的处理
        threadPoolExecutor.execute(() -> {
            messageStoreService.storeP2PMessage(messageContent);

            // 存储离线消息
            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(messageContent, offlineMessageContent);
            offlineMessageContent.setConversationType(ConversationTypeEnum.P2P.getCode());
            messageStoreService.storeOfflineMessage(offlineMessageContent);

            // 1. 回 ack 给自己
            ack(messageContent, ResponseVO.successResponse()); // 告诉自己收到了消息
            // 2. 发消息给同步在线端
            syncToSender(messageContent, new ClientInfo(messageContent.getAppId(), messageContent.getClientType(), messageContent.getImei()));
            // 3. 发消息给对方在线端
            List<ClientInfo> clientInfos = dispatchMessage(messageContent);

            messageStoreService.setMessageFromMessageIdCache(messageContent.getAppId(),
                    messageContent.getMessageId(),
                    messageContent);

            if (clientInfos.isEmpty()) {
                revicerAck(messageContent); // 告知发送此条消息的人对方收到了消息
            }
        });

        /*} else {
            // 告诉客户端失败了
            // ack
            ack(messageContent, responseVO);
        }*/

    }

    /**
     * 服务端收到消息后回一个ack,
     * 回的条件是接收消息方没有人在线
     *
     * @param messageContent
     */
    private void revicerAck(MessageContent messageContent) {
        MessageRecieveServerAckPack pack = new MessageRecieveServerAckPack();
        pack.setMessageKey(messageContent.getMessageKey());
        pack.setFromId(messageContent.getToId());
        pack.setToId(messageContent.getFromId());
        pack.setServerSend(true);
        pack.setMessageSequence(messageContent.getMessageSequence());
        messageProducer.sendToUser(messageContent.getFromId(),
                MessageCommand.MSG_RECIVE_ACK,
                pack,
                new ClientInfo(messageContent.getAppId(), messageContent.getClientType(), messageContent.getImei()));

    }

    private void ack(MessageContent messageContent, ResponseVO responseVO) {

        log.info("msg ack,msgId={},msgSeq={}, checkResult-{}", messageContent.getMessageId(), messageContent.getMessageSequence(), messageContent.getMessageBody());

        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId(),
                messageContent.getMessageSequence());

        responseVO.setData(chatMessageAck);
        // 发消息 同步在线端
        messageProducer.sendToUser(messageContent.getFromId(), MessageCommand.MSG_ACK,
                responseVO,
                new ClientInfo(messageContent.getAppId(), messageContent.getClientType(), messageContent.getImei()));

    }

    /**
     * 转发消息给对方
     *
     * @param messageContent
     */
    private List<ClientInfo> dispatchMessage(MessageContent messageContent) {
        List<ClientInfo> clientInfos = messageProducer.sendToUser(messageContent.getToId(),
                MessageCommand.MSG_P2P,
                messageContent,
                messageContent.getAppId());
        return clientInfos;
    }

    /**
     * 同步消息到自身其他端
     *
     * @param messageContent
     * @param clientInfo
     */
    private void syncToSender(MessageContent messageContent, ClientInfo clientInfo) {
        messageProducer.sendToUserExceptClient(messageContent.getFromId(),
                MessageCommand.MSG_P2P, messageContent,
                clientInfo);
    }

    public ResponseVO imServerPermissionCheck(String fromId,
                                              String toId,
                                              Integer appId) {

        ResponseVO checkSenderForbidAndMute = checkSendMessageService.checkSenderForbidAndMute(fromId, appId);
        if (!checkSenderForbidAndMute.isOk()) {
            return checkSenderForbidAndMute;
        }

        ResponseVO checkFriendShip = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return checkFriendShip;
    }

    public SendMessageResp send(SendMessageReq req) {

        SendMessageResp sendMessageResp = new SendMessageResp();
        MessageContent message = new MessageContent();
        BeanUtils.copyProperties(req, message);
        //插入数据
        messageStoreService.storeP2PMessage(message);
        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());

        //2.发消息给同步在线端
        syncToSender(message, message);
        //3.发消息给对方在线端
        dispatchMessage(message);
        return sendMessageResp;
    }

}
