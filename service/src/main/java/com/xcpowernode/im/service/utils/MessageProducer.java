package com.xcpowernode.im.service.utils;


import cn.hutool.db.Session;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.xcpower.codec.proto.MessagePack;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.command.Command;
import com.xcpower.im.model.ClientInfo;
import com.xcpower.im.model.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class MessageProducer {

    public static final Logger LOGGER = LoggerFactory.getLogger(MessageProducer.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    UserSessionUtils userSessionUtils;

    private String exchangeName = Constants.RabbitConstants.MessageService2Im; // service->im


    public boolean sendMessage(UserSession session, Object msg) {

        try {
            LOGGER.info("send message == " + msg);
            rabbitTemplate.convertAndSend(exchangeName, session.getBrokerId().toString(), msg);
            return true;
        } catch (Exception e) {
            LOGGER.error("send error : " + e.getMessage());
            return false;
        }
    }

    // 包装数据，调用sendMessage
    public boolean sendPack(String toId, Command command, Object msg, UserSession session) {
        MessagePack messagePack = new MessagePack();
        messagePack.setToId(toId);
        messagePack.setCommand(command.getCommand());
        messagePack.setClientType(session.getClientType());
        messagePack.setAppId(session.getAppId());
        messagePack.setImei(session.getImei());
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(msg));
        messagePack.setData(jsonObject);
        return sendMessage(session, JSONObject.toJSONString(messagePack));
    }

    // 发送给所有端的方法
    public List<ClientInfo> sendToUser(String toId, Command command,
                                       Object data, Integer appId) {
        List<UserSession> userSessions = userSessionUtils.getUserSession(appId, toId);
        List<ClientInfo> clientInfos = new ArrayList<>();

        for (UserSession userSession : userSessions) {

            boolean flag = sendPack(toId, command, data, userSession);
            if (flag) {
                // 如果发送成功说明存在用户不是离线状态或者不是下线状态
                clientInfos.add(new ClientInfo(userSession.getAppId(),
                        userSession.getClientType(), userSession.getImei()));
            }
        }

        return clientInfos;
    }

    // 发送给某个用户的指定客户端
    public void sendToUser(String toId, Command command,
                           Object data, ClientInfo clientInfo) {
        UserSession userSession = userSessionUtils.getUserSession(clientInfo.getAppId(), toId,
                clientInfo.getClientType(), clientInfo.getImei());
        sendPack(toId, command, data, userSession);
    }

    // 发送给除了某一端的其他端
    public void sendToUserExceptClient(String toId, Command command,
                                       Object data, ClientInfo clientInfo) {
        List<UserSession> userSessions = userSessionUtils.getUserSession(clientInfo.getAppId(),
                toId);

        List<UserSession> userSessionList = userSessions.stream()
                .filter(userSession -> !isMatch(userSession, clientInfo))
                .collect(Collectors.toList());
        for (UserSession userSession : userSessionList) {
            sendPack(toId, command, data, userSession);
        }
    }

    private boolean isMatch(UserSession sessionDto, ClientInfo clientInfo) {
        return Objects.equals(sessionDto.getAppId(), clientInfo.getAppId())
                && Objects.equals(sessionDto.getImei(), clientInfo.getImei())
                && Objects.equals(sessionDto.getClientType(), clientInfo.getClientType());
    }

    /**
     * 发送所有端和除某一端的整合方法
     *
     * @param toId
     * @param clientType
     * @param imei
     * @param command
     * @param data
     * @param appId
     */
    public void sendToUser(String toId, Integer clientType, String imei, Command command,
                           Object data, Integer appId) {
        if (clientType != null && StringUtils.isNotBlank(imei)) {
            ClientInfo clientInfo = new ClientInfo(appId, clientType, imei);
            sendToUserExceptClient(toId, command, data, clientInfo);
        } else {
            sendToUser(toId, command, data, appId);
        }
    }

}
