package com.xcpowernode.im.service.message.controller;

import com.xcpower.im.ResponseVO;
import com.xcpower.im.enums.command.MessageCommand;
import com.xcpower.im.model.SyncReq;
import com.xcpower.im.model.message.CheckSendMessageReq;
import com.xcpowernode.im.service.group.service.GroupMessageService;
import com.xcpowernode.im.service.message.model.req.SendMessageReq;
import com.xcpowernode.im.service.message.service.MessageSyncService;
import com.xcpowernode.im.service.message.service.P2PMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@RestController
@RequestMapping("v1/message")
public class MessageController {

    @Autowired
    P2PMessageService p2PMessageService;

    @Autowired
    private MessageSyncService messageSyncService;

    @RequestMapping("/send")
    public ResponseVO send(@RequestBody @Validated SendMessageReq req, Integer appId) {
        req.setAppId(appId);
        return ResponseVO.successResponse(p2PMessageService.send(req));
    }

    @RequestMapping("/checkSend")
    public ResponseVO checkSend(@RequestBody @Validated CheckSendMessageReq req) {
        return p2PMessageService.imServerPermissionCheck(req.getFromId(), req.getToId(),
                req.getAppId());
    }

    @RequestMapping("/syncOfflineMessage")
    public ResponseVO syncOfflineMessage(@RequestBody
                                         @Validated SyncReq req, Integer appId) {
        req.setAppId(appId);
        return messageSyncService.syncOfflineMessage(req);
    }

}
