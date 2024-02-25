package com.xcpowernode.im.service.friendship.controller;

import com.xcpower.im.ResponseVO;
import com.xcpowernode.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.xcpowernode.im.service.friendship.model.req.GetFriendShipRequestReq;
import com.xcpowernode.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.xcpowernode.im.service.friendship.service.ImFriendshipRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/friendshipRequest")
public class ImFriendshipRequestController {

    @Autowired
    private ImFriendshipRequestService imFriendshipRequestService;

    @RequestMapping("getFriendRequest")
    public ResponseVO getFriendRequest(@RequestBody @Validated GetFriendShipRequestReq req, Integer appId) {
        return imFriendshipRequestService.getFriendRequest(req.getFromId(), appId);
    }

    @RequestMapping("/approveFriendRequest")
    public ResponseVO approveFriendRequest(@RequestBody @Validated ApproveFriendRequestReq req,
                                            Integer appId,
                                            String identifier){
        req.setAppId(appId);
        req.setOperator(identifier);
        return imFriendshipRequestService.approverFriendRequest(req);
    }

    @RequestMapping("readFriendShipRequest")
    public ResponseVO readFriendShipRequest(@RequestBody @Validated ReadFriendShipRequestReq req,
                                            Integer appId) {
        req.setAppId(appId);
        return imFriendshipRequestService.readFriendShipRequest(req);
    }

}
