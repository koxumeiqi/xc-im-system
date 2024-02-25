package com.xcpowernode.im.service.friendship.service;

import com.xcpower.im.ResponseVO;
import com.xcpowernode.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.xcpowernode.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;

public interface ImFriendshipGroupMemberService {

    ResponseVO addGroupMember(AddFriendShipGroupMemberReq req);

    ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req);

    int doAddGroupMember(Long groupId, String toId);

    int clearGroupMember(Long groupId);

}
