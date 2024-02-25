package com.xcpowernode.im.service.user.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.xcpower.codec.pack.user.UserModifyPack;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.DelFlagEnum;
import com.xcpower.im.enums.UserErrorCode;
import com.xcpower.im.enums.command.UserEventCommand;
import com.xcpower.im.exception.ApplicationException;
import com.xcpowernode.im.service.config.AppConfig;
import com.xcpowernode.im.service.group.service.ImGroupService;
import com.xcpowernode.im.service.user.dao.ImUserDataMapper;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import com.xcpowernode.im.service.user.model.req.*;
import com.xcpowernode.im.service.user.model.res.GetUserInfoResp;
import com.xcpowernode.im.service.user.model.res.ImportUserResp;
import com.xcpowernode.im.service.user.service.ImUserService;
import com.xcpowernode.im.service.utils.CallbackService;
import com.xcpowernode.im.service.utils.MessageProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理用户相关的业务逻辑
 */
@Service
public class ImUserServiceImpl implements ImUserService {

    @Autowired
    private ImUserDataMapper imUserDataMapper;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CallbackService callbackService;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ImGroupService imGroupService;

    /**
     * 导入用户
     *
     * @param req 需导入用户的信息
     * @return
     */
    @Override
    public ResponseVO importUser(ImportUserReq req) {

        if (req.getUserData().size() > 100) {
            // TODO 返回数量过多
        }

        // 导入成功用户和导入失败用户分开响应
        List<String> successId = new ArrayList<>();
        List<String> errorId = new ArrayList<>();

        req.getUserData().forEach(e -> {
            try {
                e.setAppId(req.getAppId());
                int cnt = imUserDataMapper.insert(e);
                if (cnt == 1) {
                    successId.add(e.getUserId());
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                errorId.add(e.getUserId());
            }
        });
        ImportUserResp resp = ImportUserResp.builder()
                .sucessId(successId)
                .errorId(errorId)
                .build();
        return ResponseVO.successResponse(resp);
    }

    /**
     * 获取用户信息
     *
     * @param req
     * @return
     */
    @Override
    public ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req) {
        List<ImUserDataEntity> userInfos = imUserDataMapper.queryUserInfo(req, DelFlagEnum.NORMAL.getCode());
        // 成功查找倒的用户信息键值对（key是用户id，value是用户信息）
        final Map<String, ImUserDataEntity> successUserInfo = new HashMap<>();
        userInfos.forEach(userInfo -> successUserInfo.put(userInfo.getUserId(), userInfo));
        // 判断请求req中有哪些是错误id
        List<String> failureUserIds = new ArrayList<>();
        req.getUserIds().forEach(userId -> {
            if (!successUserInfo.containsKey(userId)) {
                failureUserIds.add(userId);
            }
        });
        GetUserInfoResp getUserInfoResp = GetUserInfoResp.builder()
                .failUser(failureUserIds)
                .userDataItem(userInfos)
                .build();
        return ResponseVO.successResponse(getUserInfoResp);
    }

    @Override
    public ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId, Integer appId) {
        ImUserDataEntity userData = imUserDataMapper.querySingleUserInfo(userId, appId, DelFlagEnum.NORMAL.getCode());
        // 返回值为空说明用户不存在
        if (ObjectUtils.isEmpty(userData)) return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        return ResponseVO.successResponse(userData);
    }

    @Override
    public ResponseVO deleteUser(DeleteUserReq deleteUserReq) {
        List<String> sucessIds = new ArrayList<>();
        List<String> failureIds = new ArrayList<>();

        for (String userId : deleteUserReq.getUserIds()) {
            try {
                int delCnt = imUserDataMapper.deleteByUserId(userId, deleteUserReq.getAppId(), DelFlagEnum.DELETE.getCode());
                if (delCnt > 0) sucessIds.add(userId);
                else failureIds.add(userId);
            } catch (Exception e) {
                failureIds.add(userId);
            }
        }
        ImportUserResp resp = ImportUserResp.builder()
                .sucessId(sucessIds)
                .errorId(failureIds)
                .build();
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO login(LoginReq req) {


        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO modifyUserInfo(ModifyUserInfoReq req) {

        ImUserDataEntity userData = imUserDataMapper.querySingleUserInfo(req.getUserId(), req.getAppId(), DelFlagEnum.NORMAL.getCode());
        if (ObjectUtils.isEmpty(userData)) {
            throw new ApplicationException(UserErrorCode.USER_IS_NOT_EXIST);
        }

        ImUserDataEntity update = new ImUserDataEntity();
        BeanUtils.copyProperties(req, update);
        int updateRes = imUserDataMapper.updateByPrimaryKey(update);

        if (updateRes == 1) {

            // 通知
            UserModifyPack pack = new UserModifyPack();
            BeanUtils.copyProperties(req, pack);
            messageProducer.sendToUser(req.getUserId(),
                    req.getClientType(), req.getImei(),
                    UserEventCommand.USER_MODIFY, pack,
                    req.getAppId());

            // 回调
            if (appConfig.isModifyUserAfterCallback()) {
                callbackService.callback(req.getAppId(),
                        Constants.CallbackCommand.ModifyUserAfter,
                        JSONObject.toJSONString(update));

            }

            return ResponseVO.successResponse();
        }

        throw new ApplicationException(UserErrorCode.MODIFY_USER_ERROR);
    }

    @Override
    public ResponseVO getUserSequence(GetUserSequenceReq req) {
        Map<Object, Object> map = redisTemplate.opsForHash().entries(req.getAppId()
                        + ":" + Constants.RedisConstants.SeqPrefix + ":" +
                req.getUserId());
        Long groupSeq = imGroupService.getUserGroupMaxSeq(req.getUserId(), req.getAppId());
        map.put(Constants.SeqConstants.Group, groupSeq);
        return ResponseVO.successResponse(map);
    }
}
