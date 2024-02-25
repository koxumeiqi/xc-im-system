package com.xcpower.tcp.redis;

import com.xcpower.codec.config.BootstrapConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

public class RedisManager {

    private static RedissonClient redissonClient;

    public static void init(BootstrapConfig config) {
        redissonClient = new SingleClientStrategy().getRedissonClient(config.getLim().getRedis());
    }

    public static RedissonClient getRedissonClient(){
        return redissonClient;
    }

}
