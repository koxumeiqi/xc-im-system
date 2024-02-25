package com.xcpower.tcp.publish;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.xcpower.codec.proto.Message;
import com.xcpower.codec.proto.MessageHeader;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.command.CommandType;
import com.xcpower.im.enums.command.GroupEventCommand;
import com.xcpower.tcp.utils.MqFactory;
import io.netty.handler.codec.json.JsonObjectDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqMessageProducer {

    public static void sendMessage(Message message, Integer command) {
        Channel channel = null;
        String channelName = Constants.RabbitConstants.Im2MessageService;

        String com = command.toString();
        String commandSub = com.substring(0, 1);
        CommandType commandType = CommandType.getCommandType(commandSub);

        if(commandType == CommandType.MESSAGE){
            channelName = Constants.RabbitConstants.Im2MessageService;
        }else if(commandType == CommandType.GROUP){
            channelName = Constants.RabbitConstants.Im2GroupService;
        }else if(commandType == CommandType.FRIEND){
            channelName = Constants.RabbitConstants.Im2FriendshipService;
        }else if(commandType == CommandType.USER){
            channelName = Constants.RabbitConstants.Im2UserService;
        }

        try {
            channel = MqFactory.getChannel(channelName);

            JSONObject o = (JSONObject) JSONObject.toJSON(message.getMessagePack());
            o.put("command", command);
            o.put("appId", message.getMessageHeader().getAppId());
            o.put("clientType", message.getMessageHeader().getClientType());
            o.put("imei", message.getMessageHeader().getImei());

            channel.basicPublish(channelName, "", null,
                    o.toJSONString().getBytes());
        } catch (Exception e) {
            log.error("发送消息出现异常:{}", e.getMessage());
        }

    }

    public static void sendMessage(Object message, MessageHeader header, Integer command) {
        Channel channel = null;
        String channelName = Constants.RabbitConstants.Im2MessageService;

        String com = command.toString();
        String commandSub = com.substring(0, 1);
        CommandType commandType = CommandType.getCommandType(commandSub);

        if(commandType == CommandType.MESSAGE){
            channelName = Constants.RabbitConstants.Im2MessageService;
        }else if(commandType == CommandType.GROUP){
            channelName = Constants.RabbitConstants.Im2GroupService;
        }else if(commandType == CommandType.FRIEND){
            channelName = Constants.RabbitConstants.Im2FriendshipService;
        }else if(commandType == CommandType.USER){
            channelName = Constants.RabbitConstants.Im2UserService;
        }

        try {
            channel = MqFactory.getChannel(channelName);

            JSONObject o = (JSONObject) JSONObject.toJSON(message);
            o.put("command", command);
            o.put("appId", header.getAppId());
            o.put("clientType", header.getClientType());
            o.put("imei", header.getImei());

            channel.basicPublish(channelName, "", null,
                    o.toJSONString().getBytes());
        } catch (Exception e) {
            log.error("发送消息出现异常:{}", e.getMessage());
        }

    }


}
