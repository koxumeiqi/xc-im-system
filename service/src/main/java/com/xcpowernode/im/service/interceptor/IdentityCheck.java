package com.xcpowernode.im.service.interceptor;


import com.alibaba.fastjson.JSONObject;
import com.xcpower.im.BaseErrorCode;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.GateWayErrorCode;
import com.xcpower.im.enums.ImUserTypeEnum;
import com.xcpower.im.exception.ApplicationExceptionEnum;
import com.xcpower.im.utils.SigAPI;
import com.xcpowernode.im.service.config.AppConfig;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import com.xcpowernode.im.service.user.service.ImUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class IdentityCheck {

    private static Logger LOGGER = LoggerFactory.getLogger(IdentityCheck.class);

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    AppConfig appConfig;

    @Autowired
    ImUserService imUserService;

    public ApplicationExceptionEnum checkUserSign(String identifier,
                                                  String appId,
                                                  String userSign) {

        // 如果上次校验过，直接返回成功即可
        String cacheUserSig = stringRedisTemplate.opsForValue().get(appId + ":" + Constants.RedisConstants.userSign + ":"
                + identifier + ":" + userSign);
        if (!StringUtils.isBlank(cacheUserSig) &&
                Long.valueOf(cacheUserSig) > System.currentTimeMillis() / 1000) {
            this.setIsAdmin(identifier, Integer.valueOf(appId));
            return BaseErrorCode.SUCCESS;
        }

        // 获取秘钥
        String privateKey = appConfig.getPrivateKey();
        // 根据appid + 秘钥创建 sigApi
        SigAPI sigAPI = new SigAPI(Long.valueOf(appId), privateKey);
        // 调用 signApi 对 userSign解密
        JSONObject jsonObject = sigAPI.decodeUserSig(userSign);
        // 取出解密后的appid 和 操作人 和 过期时间做匹配，不通过则提示错误
        Long expireTime = 0L;
        Long expireSec = 0L;
        Long time = 0L;
        String decodeAppId = "";
        String decodeIdentifier = "";

        try {
            decodeAppId = jsonObject.getString("TLS.appId");
            decodeIdentifier = jsonObject.getString("TLS.identifier");
            String expire = jsonObject.get("TLS.expire").toString();
            String expireTimeStr = jsonObject.get("TLS.expireTime").toString();// 就是创建这个秘钥的当前时间
            time = Long.valueOf(expireTimeStr);
            expireSec = Long.valueOf(expire);
            expireTime = Long.valueOf(expireTimeStr) + expireSec; // 过期时间

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("checkUserSign-error:{}", e.getMessage());
        }

        if (!decodeIdentifier.equals(identifier)) {
            return GateWayErrorCode.USERSIGN_OPERATE_NOT_MATE;
        }

        if (!decodeAppId.equals(appId)) {
            return GateWayErrorCode.USERSIGN_IS_ERROR;
        }

        if (expireSec == 0L) {
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        if (expireTime < System.currentTimeMillis() / 1000) {
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }


        // appid + "xxx" + userId + sign
        String genSig = sigAPI.genUserSig(identifier, expireSec, time, null);
        if (genSig.toLowerCase().equals(userSign.toLowerCase())) {
            String key = appId + ":" + Constants.RedisConstants.userSign + ":"
                    + identifier + ":" + userSign;

            Long etime = expireTime - System.currentTimeMillis() / 1000;
            stringRedisTemplate.opsForValue().set(key,
                    expireTime.toString(), etime, TimeUnit.SECONDS);

            // 如果是管理员就设置管理员，有些事情只管理员可做
            this.setIsAdmin(identifier, Integer.valueOf(appId));

            return BaseErrorCode.SUCCESS;
        }
        return GateWayErrorCode.USERSIGN_IS_ERROR;
    }

    public void setIsAdmin(String identifier, Integer appId) {
        //去DB或Redis中查找, 后面写
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(identifier, appId);
        if (singleUserInfo.isOk()) {
            RequestHolder.set(singleUserInfo.getData().getUserType() == ImUserTypeEnum.APP_ADMIN.getCode());
        } else {
            RequestHolder.set(false);
        }
    }

}
