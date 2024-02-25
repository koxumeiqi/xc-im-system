package com.xcpowernode.im.service.conversation.controller;


import com.xcpower.im.ResponseVO;
import com.xcpower.im.model.SyncReq;
import com.xcpowernode.im.service.conversation.model.DeleteConversationReq;
import com.xcpowernode.im.service.conversation.model.UpdateConversationReq;
import com.xcpowernode.im.service.conversation.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/conversation")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @RequestMapping("/deleteConversation")
    public ResponseVO deleteConversation(@RequestBody @Validated DeleteConversationReq
                                                 req, Integer appId, String identifier) {
        req.setAppId(appId);
//        req.setOperater(identifier);
        return conversationService.deleteConversation(req);
    }

    @RequestMapping("/updateConversation")
    public ResponseVO updateConversation(@RequestBody @Validated UpdateConversationReq
                                                 req, Integer appId, String identifier) {
        req.setAppId(appId);
//        req.setOperater(identifier);
        return conversationService.updateConversation(req);
    }

    @RequestMapping("/syncConversationList")
    public ResponseVO syncFriendShipList(@RequestBody @Validated SyncReq req, Integer appId)  {
        req.setAppId(appId);
        return conversationService.syncConversationSet(req);
    }

}
