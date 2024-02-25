package com.xcpower.tcp.utils;

import com.alibaba.fastjson.JSONObject;
import com.xcpower.codec.pack.user.UserStatusChangeNotifyPack;
import com.xcpower.codec.proto.MessageHeader;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.ImConnectStatusEnum;
import com.xcpower.im.enums.command.UserEventCommand;
import com.xcpower.im.model.UserClientDto;
import com.xcpower.im.model.UserSession;
import com.xcpower.tcp.publish.MqMessageProducer;
import com.xcpower.tcp.redis.RedisManager;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionSocketHolder {

    private static final Map<UserClientDto, NioSocketChannel> CHANNELS = new ConcurrentHashMap<>();

    public static void put(Integer appId,
                           String userId,
                           Integer clientType,
                           String imei,
                           NioSocketChannel channel) {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setImei(imei);
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        CHANNELS.put(userClientDto, channel);
    }

    public static NioSocketChannel get(Integer appId,
                                       String userId,
                                       String imei,
                                       Integer clientType) {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setImei(imei);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        return CHANNELS.get(userClientDto);
    }

    public static List<NioSocketChannel> get(Integer appId, String userId) {
        Set<UserClientDto> channelInfos = CHANNELS.keySet();
        final List<NioSocketChannel> channels = new ArrayList<>();

        channelInfos.forEach(channel -> {
            if (channel.getAppId().equals(appId) && userId.equals(channel.getUserId())) {
                channels.add(CHANNELS.get(channel));
            }
        });

        return channels;
    }

    public static void remove(Integer appId, String userId, String imei, Integer clientType) {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setImei(imei);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        CHANNELS.remove(userClientDto);
    }

    public static void remove(NioSocketChannel channel) {
        CHANNELS.entrySet().stream()
                .filter(entity -> entity.getValue().equals(channel))
                .forEach(entry -> CHANNELS.remove(entry.getKey()));
    }

    public static void removeUserSession(NioSocketChannel nioSocketChannel) {
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

        SessionSocketHolder.remove(appId, userId, imei, clientType);
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<Object, Object> map = redissonClient.getMap(appId +
                Constants.RedisConstants.UserSessionConstants + userId);
        map.remove(clientType + ":" + imei);

        UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
        userStatusChangeNotifyPack.setAppId(appId);
        userStatusChangeNotifyPack.setUserId(userId);
        userStatusChangeNotifyPack.setStatus(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
        MessageHeader messageHeader = MessageHeader.builder()
                .imei(imei)
                .clientType(clientType)
                .appId(appId)
                .build();
        MqMessageProducer.sendMessage(userStatusChangeNotifyPack,
                messageHeader,
                UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());

        nioSocketChannel.close();
    }

    public static void offlineUserSession(NioSocketChannel nioSocketChannel) {
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

        SessionSocketHolder.remove(appId, userId, imei, clientType);

        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId +
                Constants.RedisConstants.UserSessionConstants + userId);
        String sessionStr = map.get(clientType + ":" + imei);

        if (sessionStr != null && sessionStr.length() > 0) {
            UserSession userSession = JSONObject.parseObject(sessionStr, UserSession.class);
            userSession.setConnectState(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
            map.put(clientType + ":" + imei, JSONObject.toJSONString(userSession));
        }

        UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
        userStatusChangeNotifyPack.setAppId(appId);
        userStatusChangeNotifyPack.setUserId(userId);
        userStatusChangeNotifyPack.setStatus(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
        MessageHeader messageHeader = MessageHeader.builder()
                .imei(imei)
                .clientType(clientType)
                .appId(appId)
                .build();
        MqMessageProducer.sendMessage(userStatusChangeNotifyPack,
                messageHeader,
                UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());

    }

}
