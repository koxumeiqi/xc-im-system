package com.xcpowernode.im.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.xcpower.codec.pack.message.MessageReadedPack;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.command.Command;
import com.xcpower.im.enums.command.GroupEventCommand;
import com.xcpower.im.enums.command.MessageCommand;
import com.xcpower.im.model.SyncReq;
import com.xcpower.im.model.SyncResp;
import com.xcpower.im.model.message.MessageReadedContent;
import com.xcpower.im.model.message.MessageReciveAckContent;
import com.xcpower.im.model.message.OfflineMessageContent;
import com.xcpowernode.im.service.conversation.service.ConversationService;
import com.xcpowernode.im.service.utils.MessageProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class MessageSyncService {

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ConversationService conversationService;

    @Autowired
    private RedisTemplate redisTemplate;

    public void receiveMark(MessageReciveAckContent messageReciveAckContent) {
        messageProducer.sendToUser(messageReciveAckContent.getToId(),
                MessageCommand.MSG_RECIVE_ACK,
                messageReciveAckContent, messageReciveAckContent.getAppId());
    }


    public void readedMark(MessageReadedContent messageContent) {
        // 消息已读
        conversationService.messageMarkRead(messageContent);
        MessageReadedPack pack = new MessageReadedPack();
        BeanUtils.copyProperties(messageContent, pack);
        this.syncToSender(pack, messageContent, MessageCommand.MSG_READED_NOTIFY);
        // 发送给对方，告知对方已读了消息
        messageProducer.sendToUser(messageContent.getToId(),
                MessageCommand.MSG_READED_RECEIPT, pack, messageContent.getAppId());
    }

    /**
     * 同步自己的其他端
     *
     * @param pack
     * @param content
     * @param command
     */
    private void syncToSender(MessageReadedPack pack, MessageReadedContent content, Command command) {
        messageProducer.sendToUserExceptClient(pack.getFromId(), command, pack,
                content);
    }


    public void groupReadMark(MessageReadedContent messageContent) {
        conversationService.messageMarkRead(messageContent);
        MessageReadedPack messageReadedPack = new MessageReadedPack();
        BeanUtils.copyProperties(messageContent, messageReadedPack);
        syncToSender(messageReadedPack, messageContent,
                GroupEventCommand.MSG_GROUP_READED_NOTIFY);// 消息已读发送同步给自己
        // 发送群消息方，不用同步已读
        if (!messageContent.getFromId().equals(messageContent.getToId())) {
            messageProducer.sendToUser(messageContent.getToId(),
                    GroupEventCommand.MSG_GROUP_READED_RECEIPT,
                    messageContent,
                    messageContent.getAppId());
        }
    }

    public ResponseVO syncOfflineMessage(SyncReq req) {

        SyncResp<OfflineMessageContent> resp = new SyncResp<>();

        String key = req.getAppId() + ":" + Constants.RedisConstants.OfflineMessage
                + ":" + req.getOperator();
        // 获取最大的seq
        Long maxSeq = 0L;
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        Set set = zSetOperations.reverseRangeWithScores(key, 0, 0);
        if (!CollectionUtils.isEmpty(set)) {
            List list = new ArrayList(set);
            DefaultTypedTuple defaultTypedTuple = (DefaultTypedTuple) list.get(0);
            maxSeq = defaultTypedTuple.getScore().longValue();
        }

        List<OfflineMessageContent> respList = new ArrayList<>();
        resp.setMaxSequence(maxSeq);
        Set<ZSetOperations.TypedTuple> querySet = zSetOperations.rangeByScoreWithScores(key,
                req.getLastSequence(), maxSeq, 0,
                req.getMaxLimit());
        for (ZSetOperations.TypedTuple<String> typedTuple : querySet) {
            String value = typedTuple.getValue();
            OfflineMessageContent offlineMessageContent = JSONObject.parseObject(value, OfflineMessageContent.class);
            respList.add(offlineMessageContent);
        }
        resp.setDataList(respList);

        if (!CollectionUtils.isEmpty(respList)) {
            OfflineMessageContent offlineMessageContent = respList.get(respList.size() - 1);
            resp.setCompleted(maxSeq < offlineMessageContent.getMessageKey());
        }

        return ResponseVO.successResponse(resp);
    }
}
