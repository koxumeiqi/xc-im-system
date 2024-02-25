package com.xcpowernode.im.service.group.controller;


import com.xcpower.im.ResponseVO;
import com.xcpower.im.model.message.CheckSendMessageReq;
import com.xcpowernode.im.service.group.service.GroupMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/group/message")
public class MessageGroupController {

    @Autowired
    private GroupMessageService groupMessageService;


    @PostMapping("/checkSend")
    public ResponseVO checkSend(@RequestBody @Validated CheckSendMessageReq req) {
        return groupMessageService.imServerPermissionCheck(req.getFromId(), req.getToId(),
                req.getAppId());
    }

}
