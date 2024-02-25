package com.xcpowernode.im.service.message.service;


import com.alibaba.fastjson.JSONObject;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.ConversationTypeEnum;
import com.xcpower.im.enums.DelFlagEnum;
import com.xcpower.im.model.message.*;
import com.xcpowernode.im.service.config.AppConfig;
import com.xcpowernode.im.service.conversation.service.ConversationService;
import com.xcpowernode.im.service.group.dao.ImGroupMessageHistoryMapper;
import com.xcpowernode.im.service.group.entity.ImGroupMessageHistory;
import com.xcpowernode.im.service.message.entity.ImMessageBody;
import com.xcpowernode.im.service.message.entity.ImMessageHistory;
import com.xcpowernode.im.service.utils.SnowflakeIdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MessageStoreService {


    @Autowired
    SnowflakeIdWorker snowflakeIdWorker;

    @Autowired
    ImGroupMessageHistoryMapper imGroupMessageHistoryMapper;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    AppConfig appConfig;

    @Autowired
    ConversationService conversationService;

    public void storeP2PMessage(MessageContent messageContent) {

        // messageContent 转换成 messageBody
        /*ImMessageBody imMessageBody = extractMessageBody(messageContent);
        // 插入 messageBody
        imMessageBodyMapper.insertSelective(imMessageBody);
        // 转化成 messageHistory
        List<ImMessageHistory> imMessageHistories = extractToP2PMessageHistory(messageContent, imMessageBody);
        // 批量插入
        imMessageHistoryMapper.insert(imMessageHistories);
        messageContent.setMessageKey(imMessageBody.getMessageKey());*/

        DoStoreP2PMessageDto dto = new DoStoreP2PMessageDto();
        dto.setMessageContent(messageContent);
        ImMessageBodyDto imMessageBody = extractMessageBodyDto(messageContent);
        dto.setMessageBodyDto(imMessageBody);
        messageContent.setMessageKey(imMessageBody.getMessageKey());
        // TODO 发送消息
        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreP2PMessage,
                "",
                JSONObject.toJSONString(dto));

    }

    @Transactional
    public void storeGroupMessage(GroupChatMessageContent messageContent) {
        // messageContent 转换成 messageBody
        ImMessageBodyDto imMessageBody = extractMessageBody(messageContent);
        // 插入 messageBody
        /*imMessageBodyMapper.insertSelective(imMessageBody);*/
        // 转换成 GroupMessageHistory
        DoStoreGroupMessageDto dto = new DoStoreGroupMessageDto();
        dto.setMessageBodyDto(imMessageBody);
        messageContent.setMessageKey(imMessageBody.getMessageKey());
        dto.setGroupChatMessageContent(messageContent);
        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreGroupMessage,
                "",
                JSONObject.toJSONString(dto));

        /*ImGroupMessageHistory imGroupMessageHistory = extractToGroupMessageHistory(messageContent, imMessageBody);
        imGroupMessageHistoryMapper.insert(imGroupMessageHistory);

        messageContent.setMessageKey(imMessageBody.getMessageKey());*/
    }

    private ImGroupMessageHistory extractToGroupMessageHistory(GroupChatMessageContent messageContent,
                                                               ImMessageBody imMessageBody) {

        ImGroupMessageHistory imGroupMessageHistory = new ImGroupMessageHistory();
        BeanUtils.copyProperties(messageContent, imGroupMessageHistory);

        imGroupMessageHistory.setGroupId(messageContent.getGroupId());
        imGroupMessageHistory.setMessageKey(imMessageBody.getMessageKey());
        imGroupMessageHistory.setCreateTime(System.currentTimeMillis());
        imGroupMessageHistory.setMessageTime(imMessageBody.getMessageTime());

        return imGroupMessageHistory;
    }

    public List<ImMessageHistory> extractToP2PMessageHistory(MessageContent messageContent,
                                                             ImMessageBody imMessageBody) {

        List<ImMessageHistory> list = new ArrayList<>();

        ImMessageHistory fromHistory = new ImMessageHistory();
        BeanUtils.copyProperties(messageContent, fromHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        fromHistory.setMessageKey(imMessageBody.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());
        fromHistory.setMessageTime(imMessageBody.getMessageTime());

        ImMessageHistory toHistory = new ImMessageHistory();
        BeanUtils.copyProperties(messageContent, toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBody.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());
        toHistory.setMessageTime(imMessageBody.getMessageTime());

        list.add(fromHistory);
        list.add(toHistory);

        return list;
    }

    public ImMessageBodyDto extractMessageBody(MessageContent messageContent) {
        ImMessageBodyDto imMessageBody = new ImMessageBodyDto();
        imMessageBody.setAppId(messageContent.getAppId());
        imMessageBody.setMessageKey(snowflakeIdWorker.nextId());
        imMessageBody.setCreateTime(System.currentTimeMillis());
        imMessageBody.setSecurityKey("");
        imMessageBody.setExtra(messageContent.getExtra());
        imMessageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
        imMessageBody.setMessageTime(messageContent.getMessageTime());
        imMessageBody.setMessageBody(messageContent.getMessageBody());
        return imMessageBody;
    }

    public ImMessageBodyDto extractMessageBodyDto(MessageContent messageContent) {
        ImMessageBodyDto imMessageBody = new ImMessageBodyDto();
        imMessageBody.setAppId(messageContent.getAppId());
        imMessageBody.setMessageKey(snowflakeIdWorker.nextId());
        imMessageBody.setCreateTime(System.currentTimeMillis());
        imMessageBody.setSecurityKey("");
        imMessageBody.setExtra(messageContent.getExtra());
        imMessageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
        imMessageBody.setMessageTime(messageContent.getMessageTime());
        imMessageBody.setMessageBody(messageContent.getMessageBody());
        return imMessageBody;
    }

    public void setMessageFromMessageIdCache(Integer appId, String messageId,
                                             Object messageContent) {
        String key = appId + ":" + Constants.RedisConstants.cacheMessage + ":"
                + messageId;
        stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(messageContent),
                300, TimeUnit.SECONDS);
    }

    public <T> T getMessageFromMessageIdCache(Integer appId, String messageId,
                                              Class<T> clazz) {
        String key = appId + ":" + Constants.RedisConstants.cacheMessage + ":"
                + messageId;
        String obj = stringRedisTemplate.opsForValue().get(key);
        return JSONObject.parseObject(obj, clazz);
    }

    /**
     * 存储单聊离线消息
     *
     * @param offlineMessageContent
     */
    public void storeOfflineMessage(OfflineMessageContent offlineMessageContent) {

        // 找到fromId的队列
        String fromKey = offlineMessageContent.getAppId() + ":" +
                Constants.RedisConstants.OfflineMessage + ":" + offlineMessageContent.getFromId();

        // 找到 toId 的队列
        String toKey = offlineMessageContent.getAppId() + ":" +
                Constants.RedisConstants.OfflineMessage + ":" + offlineMessageContent.getToId();

        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        // 判断 队列中的数据是否超过设定值
        if (operations.zCard(fromKey) > appConfig.getOfflineMessageCount()) {
            operations.removeRange(fromKey, 0, 0);
        }
        offlineMessageContent.setConversationId(
                conversationService.convertConversationId(ConversationTypeEnum.P2P.getCode(),
                        offlineMessageContent.getFromId(), offlineMessageContent.getToId()));
        // 插入数据 根据 Messagekey 作为分值
        operations.add(fromKey, JSONObject.toJSONString(offlineMessageContent),
                offlineMessageContent.getMessageKey());

        // 判断队列中的数据是否超过设定值
        if (operations.zCard(toKey) > appConfig.getOfflineMessageCount()) {
            operations.removeRange(toKey, 0, 0);
        }
        offlineMessageContent.setConversationId(
                conversationService.convertConversationId(ConversationTypeEnum.P2P.getCode(),
                        offlineMessageContent.getToId(), offlineMessageContent.getFromId()));
        operations.add(toKey, JSONObject.toJSONString(offlineMessageContent),
                offlineMessageContent.getMessageKey());

    }


    /**
     * 存储群聊离线消息
     */
    public void storeOfflineGroupMessage(OfflineMessageContent offlineMessageContent,
                                         List<String> memberIds) {
        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        offlineMessageContent.setConversationType(ConversationTypeEnum.GROUP.getCode());

        for (String memberId : memberIds) {
            String toKey = offlineMessageContent.getAppId()
                    + ":" + Constants.RedisConstants.OfflineMessage
                    + ":" + memberId;
            // 判断队列中的数据是否超过设定值
            if (operations.zCard(toKey) > appConfig.getOfflineMessageCount()) {
                operations.removeRange(toKey, 0, 0);
            }

            offlineMessageContent.setConversationId(conversationService.convertConversationId(
                    offlineMessageContent.getConversationType(),
                    memberId, offlineMessageContent.getToId()
            ));
            operations.add(toKey, JSONObject.toJSONString(offlineMessageContent),
                    offlineMessageContent.getMessageKey());
        }


    }

}
