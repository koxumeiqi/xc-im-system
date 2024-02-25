package com.xcpower.tcp.handler;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.rabbitmq.client.Channel;
import com.xcpower.codec.pack.LoginPack;
import com.xcpower.codec.pack.message.ChatMessageAck;
import com.xcpower.codec.pack.user.LoginAckPack;
import com.xcpower.codec.pack.user.UserStatusChangeNotifyPack;
import com.xcpower.codec.proto.Message;
import com.xcpower.codec.proto.MessagePack;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.ImConnectStatusEnum;
import com.xcpower.im.enums.command.GroupEventCommand;
import com.xcpower.im.enums.command.MessageCommand;
import com.xcpower.im.enums.command.SystemCommand;
import com.xcpower.im.enums.command.UserEventCommand;
import com.xcpower.im.model.UserClientDto;
import com.xcpower.im.model.UserSession;
import com.xcpower.im.model.message.CheckSendMessageReq;
import com.xcpower.tcp.feign.FeignMessageService;
import com.xcpower.tcp.publish.MqMessageProducer;
import com.xcpower.tcp.redis.RedisManager;
import com.xcpower.tcp.utils.MqFactory;
import com.xcpower.tcp.utils.SessionSocketHolder;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.net.URL;

import static com.xcpower.im.constant.Constants.RedisConstants.UserSessionConstants;

public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    public static final Logger LOGGER = LoggerFactory.getLogger(NettyServerHandler.class);


    private Integer brokerId;

    private FeignMessageService feignMessageService;

    private String url;

    public NettyServerHandler(Integer brokerId, String url) {

        this.url = url;
        this.brokerId = brokerId;
        feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .options(new Request.Options(1000, 1000))
                .target(FeignMessageService.class, url);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) throws Exception {

        Integer command = message.getMessageHeader().getCommand();
        // 登录 command
        if (command == SystemCommand.LOGIN.getCommand()) {

            LoginPack loginPack = JSON.parseObject(JSONObject.toJSONString(message.getMessagePack()),
                    new TypeReference<LoginPack>() {
                    }.getType());

            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.UserId)).set(loginPack.getUserId());
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.AppId)).set(message.getMessageHeader().getAppId());
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.ClientType)).set(message.getMessageHeader().getClientType());
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.Imei)).set(message.getMessageHeader().getImei());


            UserSession userSession = new UserSession();
            userSession.setAppId(message.getMessageHeader().getAppId());
            userSession.setClientType(message.getMessageHeader().getClientType());
            userSession.setUserId(loginPack.getUserId());
            userSession.setConnectState(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            userSession.setBrokerId(brokerId);
            userSession.setImei(message.getMessageHeader().getImei());
            try {
                userSession.setBrokerHost(InetAddress.getLocalHost().getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // TODO 存到 Redis
            // 也是会话
            RedissonClient redissonClient = RedisManager.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(message.getMessageHeader().getAppId() +
                    UserSessionConstants + loginPack.getUserId());

            // 客户端 ClientType 和 imei 号确定唯一的用户session
            map.put(message.getMessageHeader().getClientType() + ":" + message.getMessageHeader().getImei(),
                    JSONObject.toJSONString(userSession));

            // 处理多端登录
            UserClientDto userClientDto = new UserClientDto();
            userClientDto.setImei(message.getMessageHeader().getImei());
            userClientDto.setUserId(loginPack.getUserId());
            userClientDto.setAppId(message.getMessageHeader().getAppId());
            userClientDto.setClientType(message.getMessageHeader().getClientType());
            Channel channel = MqFactory.getChannel(Constants.RedisConstants.UserLoginChannel);
            // 参数1：交换机名称；参数2：路由键；参数3：传递消息额外设置；参数4：消息的具体内容
            // 处理多端登录
            channel.basicPublish(Constants.RedisConstants.UserLoginChannel,
                    "",
                    null,
                    JSONObject.toJSONString(userClientDto).getBytes());

            // 将 channel 存起来，存入会话中
            // 这个 Channel 是netty的，上面的是 rabbitmq的
            SessionSocketHolder.put(message.getMessageHeader().getAppId(),
                    loginPack.getUserId(),
                    message.getMessageHeader().getClientType(),
                    message.getMessageHeader().getImei(),
                    ((NioSocketChannel) channelHandlerContext.channel()));

            UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
            userStatusChangeNotifyPack.setAppId(message.getMessageHeader().getAppId());
            userStatusChangeNotifyPack.setUserId(loginPack.getUserId());
            userStatusChangeNotifyPack.setStatus(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            // TODO 发送给mq
            MqMessageProducer.sendMessage(userStatusChangeNotifyPack, message.getMessageHeader(),
                    UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());

            //TODO 补充登录ack
            MessagePack<LoginAckPack> loginSuccess = new MessagePack<>();
            LoginAckPack loginAckPack = new LoginAckPack();
            loginAckPack.setUserId(loginPack.getUserId());
            loginSuccess.setCommand(SystemCommand.LOGIN.getCommand());
            loginSuccess.setData(loginAckPack);
            loginSuccess.setImei(message.getMessageHeader().getImei());
            loginSuccess.setAppId(message.getMessageHeader().getAppId());
            loginSuccess.setClientType(message.getMessageHeader().getClientType());
            channelHandlerContext.channel().writeAndFlush(loginSuccess);

        } else if (command == SystemCommand.LOGOUT.getCommand()) {
            // 删除session
            // Redis 删除
            // 关闭channel，断开连接
            SessionSocketHolder.removeUserSession(((NioSocketChannel) channelHandlerContext.channel()));
        } else if (command == SystemCommand.PING.getCommand()) {
            channelHandlerContext.channel().attr(AttributeKey.valueOf(Constants.ReadTime))
                    .set(System.currentTimeMillis());
        } else if (command == MessageCommand.MSG_P2P.getCommand()
                || command == GroupEventCommand.MSG_GROUP.getCommand()) {

            CheckSendMessageReq req = new CheckSendMessageReq();
            req.setAppId(message.getMessageHeader().getAppId());
            req.setCommand(message.getMessageHeader().getCommand());
            com.alibaba.fastjson2.JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(message.getMessagePack()));
            String fromId = jsonObject.getString("fromId");
            if (command == MessageCommand.MSG_P2P.getCommand()) {
                String toId = jsonObject.getString("toId");
                req.setToId(toId);
            } else {
                String groupId = jsonObject.getString("groupId");
                req.setToId(groupId);
            }
            req.setFromId(fromId);

            // TODO 1. 调用校验消息发送方的接口.
            ResponseVO responseVO = ResponseVO.errorResponse();
            if (command == MessageCommand.MSG_P2P.getCommand())
                responseVO = feignMessageService.checkSendMessage(req);
            else
                responseVO = feignMessageService.checkSend(req);
            // 如果成功投递到 mq
            if (responseVO.isOk()) {
                MqMessageProducer.sendMessage(message, command);
            }
            // 失败则直接ack
            else {

                // TODO ACK
                Integer ackCommand = 0;
                if (command == MessageCommand.MSG_P2P.getCommand()) {
                    ackCommand = MessageCommand.MSG_ACK.getCommand();
                } else {
                    ackCommand = GroupEventCommand.GROUP_MSG_ACK.getCommand();
                }
                ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString("messageId"));
                responseVO.setData(chatMessageAck);
                MessagePack<ResponseVO> ack = new MessagePack<>();
                ack.setData(responseVO);
                ack.setCommand(ackCommand);
                channelHandlerContext.channel().writeAndFlush(ack);
                /*Channel channel = MqFactory.getChannel(Constants.RabbitConstants.MessageService2Im + brokerId);
                // 参数1：交换机名称；参数2：路由键；参数3：传递消息额外设置；参数4：消息的具体内容
                channel.basicPublish(Constants.RabbitConstants.MessageService2Im, brokerId.toString(),
                        null, ack.toString().getBytes());*/
            }

        } else {
            MqMessageProducer.sendMessage(message, command);
            System.out.println("111");
        }

    }
}
