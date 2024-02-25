package com.xcpowernode.im.service.utils;


import com.alibaba.fastjson.JSONObject;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.ImConnectStatusEnum;
import com.xcpower.im.model.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class UserSessionUtils {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    // 1， 获取用户所有的session
    public List<UserSession> getUserSession(Integer appId, String userId) {

        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants +
                userId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(userSessionKey);
        List<UserSession> list = new ArrayList<>();

        Collection<Object> values = entries.values();
        for (Object value : values) {
            String s = (String) value;
            UserSession userSession = JSONObject.parseObject(s, UserSession.class);
            if (userSession.getConnectState() == ImConnectStatusEnum.ONLINE_STATUS.getCode()) {
                list.add(userSession);
            }
        }
        return list;
    }

    // 获取单个用户session
    public UserSession getUserSession(Integer appId, String userId,
                                      Integer clientType, String imei) {
        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants +
                userId;
        Object o = stringRedisTemplate.opsForHash().get(userSessionKey, clientType + ":" + imei);
        UserSession userSession = JSONObject.parseObject(o.toString(), UserSession.class);
        return userSession;
    }

    // 2. 获取用户除了本端的session
}
