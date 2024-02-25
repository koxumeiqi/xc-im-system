package com.xcpowernode.im.service.user.service;

import com.xcpower.im.ResponseVO;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import com.xcpowernode.im.service.user.model.req.*;
import com.xcpowernode.im.service.user.model.res.GetUserInfoResp;

public interface ImUserService {

    ResponseVO importUser(ImportUserReq req);

    ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req);

    ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId , Integer appId);

    ResponseVO deleteUser(DeleteUserReq deleteUserReq);

    ResponseVO login(LoginReq req);

    ResponseVO modifyUserInfo(ModifyUserInfoReq req);

    ResponseVO getUserSequence(GetUserSequenceReq req);
}
