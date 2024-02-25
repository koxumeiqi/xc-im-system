package com.xcpowernode.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xcpower.codec.pack.friendship.AddFriendGroupMemberPack;
import com.xcpower.codec.pack.friendship.DeleteFriendGroupMemberPack;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.enums.command.FriendshipEventCommand;
import com.xcpower.im.model.ClientInfo;
import com.xcpowernode.im.service.friendship.dao.ImFriendshipGroupMemberMapper;
import com.xcpowernode.im.service.friendship.entity.ImFriendshipGroup;
import com.xcpowernode.im.service.friendship.entity.ImFriendshipGroupMemberKey;
import com.xcpowernode.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.xcpowernode.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;
import com.xcpowernode.im.service.friendship.service.ImFriendshipGroupMemberService;
import com.xcpowernode.im.service.friendship.service.ImFriendshipGroupService;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import com.xcpowernode.im.service.user.service.ImUserService;
import com.xcpowernode.im.service.utils.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImFriendshipGroupMemberServiceImpl implements ImFriendshipGroupMemberService {

    @Autowired
    private ImFriendshipGroupMemberMapper imFriendshipGroupMemberMapper;

    @Autowired
    private ImFriendshipGroupService imFriendShipGroupService;

    @Autowired
    private ImUserService imUserService;

    @Autowired
    private MessageProducer messageProducer;

    @Override
    @Transactional
    public ResponseVO addGroupMember(AddFriendShipGroupMemberReq req) {
        ResponseVO<ImFriendshipGroup> group = imFriendShipGroupService
                .getGroup(req.getFromId(), req.getGroupName(), req.getAppId());

        // 如果不存在这个分组 那直接返回
        if (!group.isOk()) {
            return group;
        }

        List<String> successId = new ArrayList<>();
        for (String toId : req.getToIds()) {
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            if (singleUserInfo.isOk()) {
                int i = this.doAddGroupMember(group.getData().getGroupId().longValue(), toId);
                if (i == 1) {
                    successId.add(toId);
                }
            }
        }

        // TCP 通知
        Long seq = imFriendShipGroupService.updateSeq(req.getFromId(), req.getGroupName(), req.getAppId());
        AddFriendGroupMemberPack pack = new AddFriendGroupMemberPack();
        pack.setFromId(req.getFromId());
        pack.setSequence(seq);
        pack.setGroupName(req.getGroupName());
        pack.setToIds(successId);
        messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_MEMBER_ADD,
                pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));


        // 将分组后的成功的 userId 返回
        return ResponseVO.successResponse(successId);
    }

    @Override
    @Transactional
    public ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req) {
        ResponseVO<ImFriendshipGroup> group = imFriendShipGroupService
                .getGroup(req.getFromId(), req.getGroupName(), req.getAppId());
        // 判读组是否存在
        if (!group.isOk()) {
            return group;
        }

        List<String> successId = new ArrayList<>();
        for (String toId : req.getToIds()) {
            // 判断是否是自己好友
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            if (singleUserInfo.isOk()) {
                int i = deleteGroupMember(group.getData().getGroupId().longValue(), toId);
                if (i == 1) {
                    successId.add(toId);
                }
            }
        }
        // TCP 通知
        Long seq = imFriendShipGroupService.updateSeq(req.getFromId(), req.getGroupName(), req.getAppId());
        DeleteFriendGroupMemberPack pack = new DeleteFriendGroupMemberPack();
        pack.setFromId(req.getFromId());
        pack.setGroupName(req.getGroupName());
        pack.setToIds(successId);
        pack.setSequence(seq);
        messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_MEMBER_DELETE,
                pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

        return ResponseVO.successResponse(successId);
    }

    public int deleteGroupMember(Long groupId, String toId) {
        ImFriendshipGroupMemberKey imFriendshipGroupMemberKey = new ImFriendshipGroupMemberKey();
        imFriendshipGroupMemberKey.setGroupId(groupId);
        imFriendshipGroupMemberKey.setToId(toId);
        try {
            int delete = imFriendshipGroupMemberMapper.deleteByPrimaryKey(imFriendshipGroupMemberKey);
            return delete;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int doAddGroupMember(Long groupId, String toId) {
        ImFriendshipGroupMemberKey imFriendShipGroupMemberEntity = new ImFriendshipGroupMemberKey();
        imFriendShipGroupMemberEntity.setGroupId(groupId);
        imFriendShipGroupMemberEntity.setToId(toId);

        try {
            int insert = imFriendshipGroupMemberMapper.insert(imFriendShipGroupMemberEntity);
            return insert;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int clearGroupMember(Long groupId) {
        int deleteCnt = imFriendshipGroupMemberMapper.deleteByGroupId(groupId);
        return deleteCnt;
    }
}
