package com.xcpowernode.im.service.group.mq;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.GroupMemberRoleEnum;
import com.xcpower.im.enums.command.GroupEventCommand;
import com.xcpower.im.model.message.GroupChatMessageContent;
import com.xcpower.im.model.message.MessageContent;
import com.xcpower.im.model.message.MessageReadedContent;
import com.xcpowernode.im.service.group.service.GroupMessageService;
import com.xcpowernode.im.service.message.service.MessageSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

@Component
@Slf4j
public class GroupChatOperateReceiver {

    @Autowired
    GroupMessageService groupMessageService;

    @Autowired
    MessageSyncService messageSyncService;

    @RabbitListener(
            bindings = @QueueBinding( // im->service
                    value = @Queue(value = Constants.RabbitConstants.Im2GroupService, durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.Im2GroupService, durable = "true")
            ), concurrency = "1" // 一次拉取的消息数
    )
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String, Object> headers,
                              Channel channel) throws IOException {
        String msg = new String(message.getBody(), Charset.forName("utf-8"));
        log.info("CHAT MSG FROM QUEUE ::: {}", msg);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

        try {
            JSONObject o = JSONObject.parseObject(msg);
            Integer command = o.getInteger("command");
            if (command == GroupEventCommand.MSG_GROUP.getCommand()) {
                // 处理消息
                GroupChatMessageContent messageContent = o.toJavaObject(GroupChatMessageContent.class);
                groupMessageService.process(messageContent);
            } else if (command == GroupEventCommand.MSG_GROUP_READED.getCommand()) {
                MessageReadedContent messageContent = JSONObject.parseObject(msg,new TypeReference<MessageReadedContent>(){
                }.getType());
                messageSyncService.groupReadMark(messageContent);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理消息出现异常：{}", e.getMessage());
            log.error("RMQ_CHAT_TRAN_ERROR", e);
            log.error("NACK_MSG:{}", msg);
            //第一个false 表示不批量拒绝，第二个false表示不重回队列
            channel.basicNack(deliveryTag, false, false);
        }

    }


}
