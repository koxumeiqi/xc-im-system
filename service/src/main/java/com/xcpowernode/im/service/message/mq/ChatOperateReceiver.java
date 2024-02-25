package com.xcpowernode.im.service.message.mq;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.GroupErrorCode;
import com.xcpower.im.enums.command.GroupEventCommand;
import com.xcpower.im.enums.command.MessageCommand;
import com.xcpower.im.model.message.MessageContent;
import com.xcpower.im.model.message.MessageReadedContent;
import com.xcpower.im.model.message.MessageReciveAckContent;
import com.xcpowernode.im.service.message.service.MessageSyncService;
import com.xcpowernode.im.service.message.service.P2PMessageService;
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
public class ChatOperateReceiver {

    @Autowired
    P2PMessageService p2PMessageService;

    @Autowired
    MessageSyncService messageSyncService;


    @RabbitListener(
            bindings = @QueueBinding( // im->service
                    value = @Queue(value = Constants.RabbitConstants.Im2MessageService, durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.Im2MessageService, durable = "true")
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
            if (command == MessageCommand.MSG_P2P.getCommand()) {
                // 处理消息
                MessageContent messageContent = o.toJavaObject(MessageContent.class);
                p2PMessageService.process(messageContent);
            } else if (command == MessageCommand.MSG_RECIVE_ACK.getCommand()) {
                // 接收方收到消息回 ack
                MessageReciveAckContent messageContent = o.toJavaObject(MessageReciveAckContent.class);
                messageSyncService.receiveMark(messageContent);
            } else if (command == MessageCommand.MSG_READED.getCommand()) {
                // 消息已读
                MessageReadedContent messageContent = o.toJavaObject(MessageReadedContent.class);
                // 已读消息分发
                messageSyncService.readedMark(messageContent);
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
