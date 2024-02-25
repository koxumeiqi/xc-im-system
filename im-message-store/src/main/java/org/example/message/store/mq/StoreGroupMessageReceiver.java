package org.example.message.store.mq;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.xcpower.im.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.example.message.store.entity.ImMessageBody;
import org.example.message.store.model.DoStoreGroupMessageDto;
import org.example.message.store.model.DoStoreP2PMessageDto;
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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class StoreGroupMessageReceiver {

    @Autowired
    private StoreMessageService storeMessageService;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = Constants.RabbitConstants.StoreGroupMessage, durable = "true"),
            exchange = @Exchange(value = Constants.RabbitConstants.StoreGroupMessage, durable = "true")
    ), concurrency = "1")
    public void storeGroupMessage(@Payload Message message,
                                  @Headers Map<String, Object> headers,
                                  Channel channel) throws IOException {
        String msg = new String(message.getBody(), "utf-8");
        log.info("CHAT MSG FORM QUEUE ::: {}", msg);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        try {
            JSONObject jsonObject = JSON.parseObject(msg);
            DoStoreGroupMessageDto dto = jsonObject.toJavaObject(DoStoreGroupMessageDto.class);
            ImMessageBody messageBody = jsonObject.getObject("messageBodyDto", ImMessageBody.class);
            dto.setMessageBody(messageBody);
            storeMessageService.doStoreGroupMessage(dto);
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
