package com.xcpowernode.im.service.friendship.service;

import com.xcpower.im.ResponseVO;
import com.xcpowernode.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.xcpowernode.im.service.friendship.model.req.FriendDto;
import com.xcpowernode.im.service.friendship.model.req.ReadFriendShipRequestReq;

public interface ImFriendshipRequestService {

    ResponseVO addFriendshipRequest(String fromId, FriendDto dto, Integer appId);

    ResponseVO approverFriendRequest(ApproveFriendRequestReq req);

    ResponseVO getFriendRequest(String fromId, Integer appId);

    ResponseVO readFriendShipRequest(ReadFriendShipRequestReq req);


}
