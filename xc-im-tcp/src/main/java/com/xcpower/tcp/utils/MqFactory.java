package com.xcpower.tcp.utils;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.AMQImpl;
import com.xcpower.codec.config.BootstrapConfig;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class MqFactory {

    private static ConnectionFactory factory = null;

    private static Channel defaultChannel;

    private static ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();

    public static void init(BootstrapConfig.Rabbitmq rabbitmq){
        if(factory == null){
            factory = new ConnectionFactory();
            factory.setHost(rabbitmq.getHost());
            factory.setPort(rabbitmq.getPort());
            factory.setUsername(rabbitmq.getUserName());
            factory.setPassword(rabbitmq.getPassword());
            factory.setVirtualHost(rabbitmq.getVirtualHost());
        }
    }

    public static Channel getChannel(String channelName) throws IOException, TimeoutException {
        Channel channel = channelMap.get(channelName);
        if(channel == null){
            Channel channel1 = getConnection().createChannel();
            channelMap.put(channelName,channel1);
            return channel1;
        }
        return channel;
    }

    public static Connection getConnection() throws IOException, TimeoutException {
        return factory.newConnection();
    }

}
