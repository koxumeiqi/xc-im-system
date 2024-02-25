package org.example.message.store.mq;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.xcpower.im.constant.Constants;
import org.example.message.store.entity.ImMessageBody;
import org.example.message.store.model.DoStoreP2PMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.example.message.store.service.StoreMessageService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@Service
@Slf4j
public class StoreP2PMessageReceiver {


    @Autowired
    StoreMessageService storeMessageService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitConstants.StoreP2PMessage, durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.StoreP2PMessage, durable = "true")
            ), concurrency = "1"
    )
    public void storeP2PMessage(@Payload Message message,
                                @Headers Map<String, Object> headers,
                                Channel channel) throws IOException {
        String msg = new String(message.getBody(), "utf-8");
        log.info("CHAT MSG FORM QUEUE ::: {}", msg);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        try {
            JSONObject jsonObject = JSON.parseObject(msg);
            DoStoreP2PMessageDto dto = jsonObject.toJavaObject(DoStoreP2PMessageDto.class);
            ImMessageBody messageBody = jsonObject.getObject("messageBodyDto", ImMessageBody.class);
            dto.setMessageBody(messageBody);
            storeMessageService.doStoreP2PMessage(dto);
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
