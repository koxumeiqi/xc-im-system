package com.xcpowernode.im.service.group.service;

import com.xcpower.im.ResponseVO;
import com.xcpowernode.im.service.group.model.req.*;
import com.xcpowernode.im.service.group.model.resp.GetRoleInGroupResp;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface ImGroupMemberService {

    ResponseVO importGroupMember(ImportGroupMemberReq req);

    ResponseVO addMember(AddGroupMemberReq req);

    ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto);

    ResponseVO removeMember(RemoveGroupMemberReq req);

    ResponseVO removeGroupMember(String groupId, Integer appId, String memberId);

    ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId);

    ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId);

    ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req);

    boolean isGroupMember(String groupId, String memberId, Integer appId);

    ResponseVO transferGroupMember(String owner, String groupId, Integer appId);

    ResponseVO exitGroup(ExitGroupReq req);

    ResponseVO updateGroupMember(UpdateGroupMemberReq req);

    ResponseVO speak(SpeakMemberReq req);

    List<String> getGroupMemberId(String groupId, Integer appId);

    List<GroupMemberDto> getGroupManager(String groupId, Integer appId);

    ResponseVO<Collection<String>> syncMemberJoinedGroup(String operator, Integer appId);

}
