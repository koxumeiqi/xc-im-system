package com.xcpower.tcp.reciver;


import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.xcpower.codec.proto.Message;
import com.xcpower.codec.proto.MessagePack;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.ClientType;
import com.xcpower.im.enums.DeviceMultiLoginEnum;
import com.xcpower.im.enums.command.SystemCommand;
import com.xcpower.im.model.UserClientDto;
import com.xcpower.tcp.utils.MqFactory;
import com.xcpower.tcp.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @description: 多端同步：1单端登录：一端在线：踢掉除了本clinetType + imel 的设备
 * 2双端登录：允许pc/mobile 其中一端登录 + web端 踢掉除了本clinetType + imel 以外的web端设备
 * 3 三端登录：允许手机+pc+web，踢掉同端的其他imei 除了web
 * 4 不做任何处理
 **/
public class UserLoginMessageListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserLoginMessageListener.class);

    private Integer loginModel;

    public UserLoginMessageListener(Integer loginModel) {
        this.loginModel = loginModel;
    }

    public void listenerUserLogin() throws IOException, TimeoutException {
        Channel channel = MqFactory.getChannel(Constants.RedisConstants.UserLoginChannel);
        String queueName = channel.queueDeclare().getQueue(); // 临时队列
        // 声明交换机
        channel.exchangeDeclare(Constants.RedisConstants.UserLoginChannel, "fanout", true);
        // 绑定交换机（1：队列名；2：交换机名；3：路由名
        channel.queueBind(queueName, Constants.RedisConstants.UserLoginChannel, "");

        channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body);
                LOGGER.info("收到用户上线通知：{}", msg);
                UserClientDto dto = JSONObject.parseObject(msg, UserClientDto.class);
                List<NioSocketChannel> nioSocketChannels = SessionSocketHolder.get(dto.getAppId(),
                        dto.getUserId());

                for (NioSocketChannel nioSocketChannel : nioSocketChannels) {
                    Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                    String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                    // 多段登录1：踢掉除自身外的登录设备
                    if (loginModel == DeviceMultiLoginEnum.ONE.getLoginMode()) {
                        if (!(clientType + ":" + imei).equals(dto.getClientType() + ":" + dto.getImei())) {
                            // 踢掉踢掉,告诉客户端要断连
                            kickOffClient(nioSocketChannel);
                        }
                    }
                    //双端登录：允许pc/mobile 其中一端登录 + web端 踢掉除了本clinetType + imel 以外的web端设备
                    else if (loginModel == DeviceMultiLoginEnum.TWO.getLoginMode()) {
                        if (dto.getClientType() == ClientType.WEB.getCode() && clientType == ClientType.WEB.getCode()) {
                            continue;
                        }
                        if (!(clientType + ":" + imei).equals(dto.getClientType() + ":" + dto.getImei())) {
                            // 踢掉踢掉
                            kickOffClient(nioSocketChannel);
                        }
                    }
                    // 三端登录：允许手机+pc+web，踢掉同端的其他imei 除了web
                    else if (loginModel == DeviceMultiLoginEnum.THREE.getLoginMode()) {
                        if (dto.getClientType() == ClientType.WEB.getCode()) {
                            continue;
                        }

                        boolean isSameClient = false;
                        if ((clientType == ClientType.IOS.getCode() ||
                                clientType == ClientType.ANDROID.getCode()) &&
                                (dto.getClientType() == ClientType.IOS.getCode() ||
                                        dto.getClientType() == ClientType.ANDROID.getCode())) {
                            isSameClient = true;
                        }

                        if ((clientType == ClientType.MAC.getCode() ||
                                clientType == ClientType.WINDOWS.getCode()) &&
                                (dto.getClientType() == ClientType.MAC.getCode() ||
                                        dto.getClientType() == ClientType.WINDOWS.getCode())) {
                            isSameClient = true;
                        }
                        if (isSameClient && !(clientType + ":" + imei).equals(dto.getClientType() + ":" + dto.getImei())) {
                            // 踢掉踢掉
                            kickOffClient(nioSocketChannel);
                        }
                    }
                }

            }
        });
    }

    private void kickOffClient(NioSocketChannel nioSocketChannel) {
        MessagePack<Object> pack = new MessagePack<>();
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        pack.setToId(userId);
        pack.setUserId(userId);
        pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
        nioSocketChannel.writeAndFlush(pack);
    }

}
