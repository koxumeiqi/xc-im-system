package com.xcpowernode.im.service.user.controller;

import com.xcpower.im.ResponseVO;
import com.xcpowernode.im.service.user.model.req.GetUserInfoReq;
import com.xcpowernode.im.service.user.model.req.ModifyUserInfoReq;
import com.xcpowernode.im.service.user.model.req.UserId;
import com.xcpowernode.im.service.user.service.ImUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/user/data")
public class ImUserDataController {

    @Autowired
    private ImUserService imUserService;


    @RequestMapping("/getUserInfo")
    public ResponseVO getUserInfo(@RequestBody GetUserInfoReq req,Integer appId){
        req.setAppId(appId);
        return imUserService.getUserInfo(req);
    }

    @RequestMapping("/getSingleInfo")
    public ResponseVO getSingleUserInfo(@RequestBody @Validated UserId userId,
                                        Integer appId){
        userId.setAppId(appId);
        return imUserService.getSingleUserInfo(userId.getUserId(),appId);
    }

    @RequestMapping("/modifyUserInfo")
    public ResponseVO modifyUserInfo(@RequestBody @Validated ModifyUserInfoReq req, Integer appId){
        req.setAppId(appId);
        return imUserService.modifyUserInfo(req);
    }

}
