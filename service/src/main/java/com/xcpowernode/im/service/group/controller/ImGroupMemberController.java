package com.xcpowernode.im.service.group.controller;


import com.xcpower.im.ResponseVO;
import com.xcpowernode.im.service.group.model.req.*;
import com.xcpowernode.im.service.group.service.ImGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/group/member")
public class ImGroupMemberController {

    @Autowired
    private ImGroupMemberService imGroupMemberService;

    @RequestMapping("/importGroupMember")
    public ResponseVO importGroupMember(@RequestBody @Validated ImportGroupMemberReq req,
                                        Integer appId,
                                        String identifier)  {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupMemberService.importGroupMember(req);
    }

    @RequestMapping("/add")
    public ResponseVO addMember(@RequestBody @Validated AddGroupMemberReq req,
                                Integer appId,
                                String identifier)  {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupMemberService.addMember(req);
    }

    @RequestMapping("/remove")
    public ResponseVO removeMember(@RequestBody @Validated RemoveGroupMemberReq req,
                                   Integer appId,
                                   String identifier)  {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupMemberService.removeMember(req);
    }

    @RequestMapping("/exitGroup")
    public ResponseVO exitGroup(@RequestBody @Validated ExitGroupReq req,
                                Integer appId,
                                String identifier){
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupMemberService.exitGroup(req);
    }

    @RequestMapping("/update")
    public ResponseVO updateGroupMember(@RequestBody @Validated UpdateGroupMemberReq req,
                                        Integer appId,
                                        String identifier)  {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupMemberService.updateGroupMember(req);
    }

    @RequestMapping("/speak")
    public ResponseVO speak(@RequestBody @Validated SpeakMemberReq req,
                            Integer appId,
                            String identifier)  {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imGroupMemberService.speak(req);
    }

}
