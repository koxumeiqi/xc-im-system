package com.xcpowernode.im.service.friendship.service;

import com.xcpower.im.ResponseVO;
import com.xcpowernode.im.service.friendship.entity.ImFriendshipGroup;
import com.xcpowernode.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.xcpowernode.im.service.friendship.model.req.DeleteFriendShipGroupReq;
import com.xcpowernode.im.service.group.model.req.UpdateGroupReq;

public interface ImFriendshipGroupService {

    ResponseVO addGroup(AddFriendShipGroupReq req);

    ResponseVO deleteGroup(DeleteFriendShipGroupReq req);

    ResponseVO<ImFriendshipGroup> getGroup(String fromId, String groupName, Integer appId);

    Long updateSeq(String fromId, String groupName, Integer appId);

}
