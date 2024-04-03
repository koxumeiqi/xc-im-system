package com.xcpower.tcp.reciver;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.xcpower.codec.proto.MessagePack;
import com.xcpower.im.constant.Constants;
import com.xcpower.tcp.reciver.process.BaseProcess;
import com.xcpower.tcp.reciver.process.ProcessFactory;
import com.xcpower.tcp.utils.MqFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.TimeoutException;


@Slf4j
public class MessageReciver {

    public static String brokerId;

    private static void startReciverMessage() {
        try {
            Channel channel = MqFactory.getChannel(Constants.RabbitConstants.MessageService2Im + brokerId);
            channel.queueDeclare(Constants.RabbitConstants.MessageService2Im + brokerId,
                    false, false,false,null);
            channel.queueBind(Constants.RabbitConstants.MessageService2Im + brokerId,
                    Constants.RabbitConstants.MessageService2Im,
                    brokerId);

            // 取消自动消费，防止消息漏消费
            channel.basicConsume(Constants.RabbitConstants.MessageService2Im + brokerId,false,
                    new DefaultConsumer(channel){
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            // TODO 处理消息服务发来的消息
                            try {
                                String msgStr = new String(body);
                                MessagePack messagePack = JSONObject.parseObject(msgStr, MessagePack.class);
                                BaseProcess messageProcess = ProcessFactory.getMessageProcess(messagePack.getCommand());
                                messageProcess.process(messagePack);
                                log.info(msgStr);


                                channel.basicAck(envelope.getDeliveryTag(), false);
                            }catch (Exception e){
                                e.printStackTrace();
                                channel.basicNack(envelope.getDeliveryTag(), false,false);
                            }
                        }
                    });

        }catch(Exception e){
        }
    }

    public static void init(){
        startReciverMessage();
    }

    public static void init(String brokerId){
        if (StringUtils.isEmpty(MessageReciver.brokerId)) {
            MessageReciver.brokerId = brokerId;
        }
        startReciverMessage();
    }

}
