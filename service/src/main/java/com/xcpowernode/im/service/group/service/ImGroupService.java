package com.xcpowernode.im.service.group.service;

import com.xcpower.im.ResponseVO;
import com.xcpower.im.model.SyncReq;
import com.xcpowernode.im.service.group.model.req.*;

public interface ImGroupService {

    ResponseVO importGroup(ImportGroupReq req);

    ResponseVO getGroup(String groupId,Integer appId);

    ResponseVO getGroup(GetGroupReq req);

    ResponseVO getJoinedGroup(GetJoinedGroupReq req);

    ResponseVO createGroup(CreateGroupReq req);

    ResponseVO updateBaseGroupInfo(UpdateGroupReq req);

    ResponseVO destroyGroup(DestroyGroupReq req);

    ResponseVO muteGroup(MuteGroupReq req);

    ResponseVO transferGroup(TransferGroupReq req);

    ResponseVO syncJoinedGroupList(SyncReq req);

    Long getUserGroupMaxSeq(String userId, Integer appId);
}
