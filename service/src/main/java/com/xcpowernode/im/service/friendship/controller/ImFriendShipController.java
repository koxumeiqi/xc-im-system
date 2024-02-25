package com.xcpowernode.im.service.friendship.controller;


import com.xcpower.im.ResponseVO;
import com.xcpower.im.model.SyncReq;
import com.xcpowernode.im.service.friendship.model.req.*;
import com.xcpowernode.im.service.friendship.service.ImFriendShipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/friendship")
public class ImFriendShipController {

    @Autowired
    private ImFriendShipService imFriendShipService;

    @RequestMapping("/importFriendShip")
    public ResponseVO importFriends(@RequestBody @Validated ImportFriendShipReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.importFriendShip(req);
    }

    @RequestMapping("/updateFriend")
    public ResponseVO updateFriend(@RequestBody @Validated UpdateFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.updateFriend(req);
    }

    @RequestMapping("/addFriend")
    public ResponseVO addFriend(@RequestBody @Validated AddFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.addFriend(req);
    }

    @PostMapping("/deleteFriend")
    public ResponseVO deleteFriend(@RequestBody @Validated DeleteFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.deleteFriend(req);
    }

    @PostMapping("/deleteAllFriend")
    public ResponseVO deleteAllFriend(@RequestBody @Validated DeleteFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.deleteAllFriend(req);
    }

    @RequestMapping("/getRelation")
    public ResponseVO getRelation(@RequestBody @Validated GetRelationReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.getRelation(req);
    }

    @RequestMapping("/getAllFriendShip")
    public ResponseVO getAllFriendShip(@RequestBody @Validated GetAllFriendShipReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.getAllFriendShip(req);
    }

    @RequestMapping("/chekFriendship")
    public ResponseVO chekFriendship(@RequestBody @Validated CheckFriendShipReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.chekFriendship(req);
    }

    @RequestMapping("/addBlack")
    public ResponseVO addBlack(@RequestBody @Validated AddFriendShipBlackReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.addBlack(req);
    }

    @RequestMapping("/deleteBlack")
    public ResponseVO deleteBlack(@RequestBody @Validated DeleteBlackReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.deleteBlack(req);
    }

    @RequestMapping("/checkBlack")
    public ResponseVO checkBlack(@RequestBody @Validated CheckFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.checkBlack(req);
    }

    @RequestMapping("/syncFriendshipList")
    public ResponseVO syncFriendshipList(@RequestBody @Validated
                                            SyncReq req,
                                        Integer appId){
        req.setAppId(appId);
        return imFriendShipService.syncFriendshipList(req);
    }

}
