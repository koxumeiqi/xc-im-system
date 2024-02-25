package com.xcpowernode.im.service.utils;


import com.xcpower.im.constant.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
public class WriteUserSeq {

    // redis
    // uid  friend
    //      group
    //      conversation
    @Autowired
    private RedisTemplate redisTemplate;

    public void writeUserSeq(Integer appId, String userId,
                             String type, Long seq) {
        String key = appId + ":" +
                Constants.RedisConstants.SeqPrefix +
                ":" + userId;
        redisTemplate.opsForHash().put(key, type, seq);

    }

}
