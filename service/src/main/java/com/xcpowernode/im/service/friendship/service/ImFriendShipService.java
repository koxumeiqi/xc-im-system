package com.xcpowernode.im.service.friendship.service;

import com.xcpower.im.ResponseVO;
import com.xcpower.im.model.RequestBase;
import com.xcpower.im.model.SyncReq;
import com.xcpowernode.im.service.friendship.model.req.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ImFriendShipService {

    ResponseVO importFriendShip(ImportFriendShipReq req);

    ResponseVO addFriend(AddFriendReq addFriendReq);

    ResponseVO updateFriend(UpdateFriendReq updateFriendReq);

    ResponseVO deleteFriend(DeleteFriendReq deleteFriendReq);

    ResponseVO deleteAllFriend(DeleteFriendReq deleteFriendReq);

    ResponseVO getAllFriendShip(GetAllFriendShipReq req);

    ResponseVO getRelation(GetRelationReq req);

    ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId);

    ResponseVO chekFriendship(CheckFriendShipReq checkFriendShipReq);

    ResponseVO addBlack(AddFriendShipBlackReq req);

    ResponseVO deleteBlack(DeleteBlackReq req);

    ResponseVO checkBlack(CheckFriendShipReq req);

    ResponseVO syncFriendshipList(SyncReq req);

    List<String> getAllFriendId(String userId, Integer appId);
}
