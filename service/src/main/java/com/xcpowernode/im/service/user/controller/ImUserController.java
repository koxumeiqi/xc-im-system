package com.xcpowernode.im.service.user.controller;


import com.xcpower.im.ResponseVO;
import com.xcpower.im.enums.ClientType;
import com.xcpower.im.route.RouteHandle;
import com.xcpower.im.route.RouteInfo;
import com.xcpower.im.utils.RouteInfoParseUtil;
import com.xcpowernode.im.service.user.model.req.DeleteUserReq;
import com.xcpowernode.im.service.user.model.req.GetUserSequenceReq;
import com.xcpowernode.im.service.user.model.req.ImportUserReq;
import com.xcpowernode.im.service.user.model.req.LoginReq;
import com.xcpowernode.im.service.user.service.ImUserService;
import com.xcpowernode.im.service.utils.ZKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("v1/user")
public class ImUserController {

    @Autowired
    private ImUserService imUserService;

    @Autowired
    private RouteHandle routeHandle;

    @Autowired
    private ZKit zKit;

    @PostMapping("/importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.importUser(req);
    }

    @RequestMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq deleteUserReq, Integer appId) {
        deleteUserReq.setAppId(appId);
        return imUserService.deleteUser(deleteUserReq);
    }

    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq loginReq) {

        ResponseVO loginRes = imUserService.login(loginReq);
        if (loginRes.isOk()) {
            List<String> allNodes = new ArrayList<>();
            // 负载均衡寻服务端
            if (loginReq.getClientType() == ClientType.WEB.getCode()) {
                allNodes = zKit.getAllWebNode();
            } else {
                allNodes = zKit.getAllTcpNode();
            }
            String path = routeHandle.routeServer(allNodes, loginReq.getUserId());
//            path = "192.168.31.20:18888";// 先写死，好测试
            RouteInfo routeInfo = RouteInfoParseUtil.parse(path);
            return ResponseVO.successResponse(routeInfo);
        }

        return ResponseVO.errorResponse();
    }

    @RequestMapping("/getUserSequence")
    public ResponseVO getUserSequence(@RequestBody @Validated
                                      GetUserSequenceReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserSequence(req);
    }

}
