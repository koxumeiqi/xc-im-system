package com.xcpowernode.im.service.user.mq;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rabbitmq.client.Channel;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.command.UserEventCommand;
import com.xcpowernode.im.service.user.model.UserStatusChangeNotifyContent;
import com.xcpowernode.im.service.user.service.ImUserStatusService;
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
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class UserOnlineStatusReceiver {

    @Autowired
    private ImUserStatusService imUserStatusService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitConstants.Im2UserService, durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.Im2UserService, durable = "true")
            ), concurrency = "1"
    )
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String, Object> headers,
                              Channel channel) throws IOException {
        long start = System.currentTimeMillis();
        Thread t = Thread.currentThread();
        String msg = new String(message.getBody());
        log.info("USER ONLINE STATUS FROM QUEUE :::::" + msg);
        //deliveryTag 用于回传 rabbitmq 确认该消息处理成功
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

        try {
            JSONObject jsonObject = JSON.parseObject(msg);
            Integer command = jsonObject.getInteger("command");
            if (Objects.equals(command, UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand())) {
                UserStatusChangeNotifyContent content = JSONObject.parseObject(msg, UserStatusChangeNotifyContent.class);
                //TODO
                imUserStatusService.processUserOnlineStatusNotify(content);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理消息出现异常：{}", e.getMessage());
            log.error("RMQ_CHAT_TRAN_ERROR", e);
            log.error("NACK_MSG:{}", msg);
            //第一个false 表示不批量拒绝，第二个false表示不重回队列
            channel.basicNack(deliveryTag, false, false);
        } finally {
            long end = System.currentTimeMillis();
            log.debug("channel {} basic-Ack ,it costs {} ms,threadName = {},threadId={}", channel, end - start, t.getName(), t.getId());
        }
    }


}
