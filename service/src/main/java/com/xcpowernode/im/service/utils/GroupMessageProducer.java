package com.xcpowernode.im.service.utils;


import com.alibaba.fastjson.JSONObject;
import com.xcpower.codec.pack.group.AddGroupMemberPack;
import com.xcpower.codec.pack.group.RemoveGroupMemberPack;
import com.xcpower.codec.pack.group.UpdateGroupMemberPack;
import com.xcpower.im.enums.ClientType;
import com.xcpower.im.enums.command.Command;
import com.xcpower.im.enums.command.GroupEventCommand;
import com.xcpower.im.model.ClientInfo;
import com.xcpowernode.im.service.group.model.req.GroupMemberDto;
import com.xcpowernode.im.service.group.service.ImGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroupMessageProducer {

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    public void producer(String userId, Command command, Object data,
                         ClientInfo clientInfo) {

        JSONObject o = (JSONObject) JSONObject.toJSON(data);
        String groupId = o.getString("groupId");
        List<String> memberIds = imGroupMemberService.getGroupMemberId(groupId,
                clientInfo.getAppId());

        if (command.equals(GroupEventCommand.ADDED_MEMBER)) {
            //发送给管理员和被加入人本身
            List<GroupMemberDto> groupManager = imGroupMemberService.getGroupManager(groupId, clientInfo.getAppId());
            AddGroupMemberPack addGroupMemberPack
                    = o.toJavaObject(AddGroupMemberPack.class);
            List<String> members = addGroupMemberPack.getMembers();
            for (GroupMemberDto groupMemberDto : groupManager) {
                if (clientInfo.getClientType() != ClientType.WEBAPI.getCode() && groupMemberDto.getMemberId().equals(userId)) {
                    messageProducer.sendToUserExceptClient(groupMemberDto.getMemberId(), command, data, clientInfo);
                } else {
                    messageProducer.sendToUser(groupMemberDto.getMemberId(), command, data, clientInfo.getAppId());
                }
            }
            for (String member : members) {
                if (clientInfo.getClientType() != ClientType.WEBAPI.getCode() && member.equals(userId)) {
                    messageProducer.sendToUserExceptClient(member, command, data, clientInfo);
                } else {
                    messageProducer.sendToUser(member, command, data, clientInfo.getAppId());
                }
            }
        } else if (command.equals(GroupEventCommand.DELETED_MEMBER)) {
            RemoveGroupMemberPack pack = o.toJavaObject(RemoveGroupMemberPack.class);
            String member = pack.getMember();
            List<String> members = imGroupMemberService.getGroupMemberId(groupId, clientInfo.getAppId());
            members.add(member);
            for (String memberId : members) {
                if (clientInfo.getClientType() != ClientType.WEBAPI.getCode() && member.equals(userId)) {
                    messageProducer.sendToUserExceptClient(memberId, command, data, clientInfo);
                } else {
                    messageProducer.sendToUser(memberId, command, data, clientInfo.getAppId());
                }
            }
        } else if (command.equals(GroupEventCommand.UPDATED_MEMBER)) {
            UpdateGroupMemberPack pack =
                    o.toJavaObject(UpdateGroupMemberPack.class);
            String memberId = pack.getMemberId();
            List<GroupMemberDto> groupManager = imGroupMemberService.getGroupManager(groupId, clientInfo.getAppId());
            GroupMemberDto groupMemberDto = new GroupMemberDto();
            groupMemberDto.setMemberId(memberId);
            groupManager.add(groupMemberDto);
            for (GroupMemberDto member : groupManager) {
                if (clientInfo.getClientType() != ClientType.WEBAPI.getCode() && member.equals(userId)) {
                    messageProducer.sendToUserExceptClient(member.getMemberId(), command, data, clientInfo);
                } else {
                    messageProducer.sendToUser(member.getMemberId(), command, data, clientInfo.getAppId());
                }
            }
        } else {
            for (String memberId : memberIds) {
                // 是自身操作的话就发送给除自己端以外的端，否则就发送所有端，数据同步
                if (clientInfo.getClientType() != null && clientInfo.getClientType() !=
                        ClientType.WEBAPI.getCode() && memberId.equals(userId)) {
                    messageProducer.sendToUserExceptClient(memberId, command,
                            data, clientInfo);
                } else {
                    messageProducer.sendToUser(memberId, command, data, clientInfo.getAppId());
                }
            }
        }
    }

}
