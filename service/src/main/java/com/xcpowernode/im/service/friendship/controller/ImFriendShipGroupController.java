package com.xcpowernode.im.service.friendship.controller;

import com.xcpower.im.ResponseVO;
import com.xcpowernode.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.xcpowernode.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.xcpowernode.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;
import com.xcpowernode.im.service.friendship.model.req.DeleteFriendShipGroupReq;
import com.xcpowernode.im.service.friendship.service.ImFriendshipGroupMemberService;
import com.xcpowernode.im.service.friendship.service.ImFriendshipGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Chackylee
 * @description:
 **/
@RestController
@RequestMapping("v1/friendship/group")
public class ImFriendShipGroupController {

    @Autowired
    private ImFriendshipGroupService imFriendshipGroupService;

    @Autowired
    private ImFriendshipGroupMemberService imFriendshipGroupMemberService;


    @RequestMapping("/add")
    public ResponseVO add(@RequestBody @Validated AddFriendShipGroupReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendshipGroupService.addGroup(req);
    }

    @RequestMapping("/del")
    public ResponseVO del(@RequestBody @Validated DeleteFriendShipGroupReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendshipGroupService.deleteGroup(req);
    }

    @RequestMapping("/member/add")
    public ResponseVO memberAdd(@RequestBody @Validated AddFriendShipGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendshipGroupMemberService.addGroupMember(req);
    }

    @RequestMapping("/member/del")
    public ResponseVO memberdel(@RequestBody @Validated DeleteFriendShipGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendshipGroupMemberService.delGroupMember(req);
    }


}
