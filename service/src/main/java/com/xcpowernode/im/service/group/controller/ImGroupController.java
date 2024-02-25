package com.xcpowernode.im.service.group.controller;


import com.xcpower.im.ResponseVO;
import com.xcpower.im.model.SyncReq;
import com.xcpowernode.im.service.group.model.req.*;
import com.xcpowernode.im.service.group.service.GroupMessageService;
import com.xcpowernode.im.service.group.service.ImGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/group")
public class ImGroupController {

    @Autowired
    private ImGroupService imGroupService;

    @Autowired
    private GroupMessageService groupMessageService;

    @RequestMapping("/importGroup")
    public ResponseVO importGroup(@RequestBody @Validated ImportGroupReq req, Integer appId) {
        req.setAppId(appId);
        return imGroupService.importGroup(req);
    }

    @RequestMapping("/getGroupInfo")
    public ResponseVO getGroupInfo(@RequestBody @Validated GetGroupReq req, Integer appId) {
        req.setAppId(appId);
        return imGroupService.getGroup(req);
    }

    @RequestMapping("/getJoinedGroup")
    public ResponseVO getJoinedGroup(@RequestBody @Validated GetJoinedGroupReq req,
                                     Integer appId,
                                     String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupService.getJoinedGroup(req);
    }

    @RequestMapping("/createGroup")
    public ResponseVO createGroup(@RequestBody @Validated CreateGroupReq req,
                                  Integer appId,
                                  String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupService.createGroup(req);
    }

    @RequestMapping("/update")
    public ResponseVO update(@RequestBody @Validated UpdateGroupReq req,
                             Integer appId,
                             String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupService.updateBaseGroupInfo(req);
    }

    @RequestMapping("/destroyGroup")
    public ResponseVO destroyGroup(@RequestBody @Validated DestroyGroupReq req,
                                   Integer appId,
                                   String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupService.destroyGroup(req);
    }

    @RequestMapping("/transferGroup")
    public ResponseVO transferGroup(@RequestBody @Validated TransferGroupReq req,
                                    Integer appId,
                                    String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupService.transferGroup(req);
    }

    @RequestMapping("/forbidSendMessage")
    public ResponseVO forbidSendMessage(@RequestBody @Validated MuteGroupReq req,
                                        Integer appId,
                                        String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupService.muteGroup(req);
    }

    @RequestMapping("/sendMessage")
    public ResponseVO sendMessage(@RequestBody @Validated SendGroupMessageReq
                                          req, Integer appId,
                                  String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return ResponseVO.successResponse(groupMessageService.send(req));
    }

    @RequestMapping("/syncJoinedGroup")
    public ResponseVO syncJoinedGroup(@RequestBody @Validated SyncReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        return imGroupService.syncJoinedGroupList(req);
    }


}
