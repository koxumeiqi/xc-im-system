package com.xcpower.tcp;


import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

public class RedissonTest {

    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.248.143:6379").setPassword("031119");
        StringCodec stringCodec = new StringCodec();
        config.setCodec(stringCodec);
        RedissonClient redissonClient = Redisson.create(config);

        RBucket<Object> im = redissonClient.getBucket("im");
        System.out.println(im.get());
        im.set("im");
        System.out.println(im.get());
    }

}
