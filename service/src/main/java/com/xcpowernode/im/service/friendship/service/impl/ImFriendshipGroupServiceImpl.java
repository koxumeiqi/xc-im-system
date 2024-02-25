package com.xcpowernode.im.service.friendship.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.xcpower.codec.pack.friendship.AddFriendGroupPack;
import com.xcpower.codec.pack.friendship.DeleteFriendGroupPack;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.DelFlagEnum;
import com.xcpower.im.enums.FriendShipErrorCode;
import com.xcpower.im.enums.command.FriendshipEventCommand;
import com.xcpower.im.model.ClientInfo;
import com.xcpowernode.im.service.config.AppConfig;
import com.xcpowernode.im.service.friendship.dao.ImFriendshipGroupMapper;
import com.xcpowernode.im.service.friendship.dao.ImFriendshipGroupMemberMapper;
import com.xcpowernode.im.service.friendship.entity.ImFriendshipGroup;
import com.xcpowernode.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.xcpowernode.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.xcpowernode.im.service.friendship.model.req.DeleteFriendShipGroupReq;
import com.xcpowernode.im.service.friendship.service.ImFriendshipGroupMemberService;
import com.xcpowernode.im.service.friendship.service.ImFriendshipGroupService;
import com.xcpowernode.im.service.seq.RedisSeq;
import com.xcpowernode.im.service.utils.CallbackService;
import com.xcpowernode.im.service.utils.MessageProducer;
import com.xcpowernode.im.service.utils.WriteUserSeq;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;

@Service
public class ImFriendshipGroupServiceImpl implements ImFriendshipGroupService {

    @Autowired
    private ImFriendshipGroupMapper imFriendshipGroupMapper;

    @Autowired
    private ImFriendshipGroupMemberService imFriendshipGroupMemberService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CallbackService callbackService;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private RedisSeq redisSeq;

    @Autowired
    private WriteUserSeq writeUserSeq;

    @Override
    @Transactional
    public ResponseVO addGroup(AddFriendShipGroupReq req) {
        ImFriendshipGroup imFriendshipGroup = new ImFriendshipGroup();
        imFriendshipGroup.setFromId(req.getFromId());
        imFriendshipGroup.setGroupName(req.getGroupName());
        imFriendshipGroup.setAppId(req.getAppId());
        imFriendshipGroup.setDelFlag(DelFlagEnum.NORMAL.getCode());

        ImFriendshipGroup entity = imFriendshipGroupMapper.selectOne(imFriendshipGroup);

        if (entity != null) {
            // 分组已存在不允许创建
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }


        // 写入db
        ImFriendshipGroup insert = new ImFriendshipGroup();
        insert.setAppId(req.getAppId());
        insert.setCreateTime(System.currentTimeMillis());
        insert.setDelFlag(DelFlagEnum.NORMAL.getCode());
        insert.setGroupName(req.getGroupName());
        long seq = redisSeq.doGetSeq(req.getAppId() + ":" +
                Constants.SeqConstants.FriendshipGroup);
        insert.setSequence(seq);
        insert.setFromId(req.getFromId());
        try {
            int insertRes = imFriendshipGroupMapper.insertSelective(insert);
            if (insertRes != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_CREATE_ERROR);
            }
            if (insertRes == 1 && CollectionUtil.isNotEmpty(req.getToIds())) {
                AddFriendShipGroupMemberReq addFriendShipGroupMemberReq = new AddFriendShipGroupMemberReq();
                addFriendShipGroupMemberReq.setFromId(req.getFromId());
                addFriendShipGroupMemberReq.setGroupName(req.getGroupName());
                addFriendShipGroupMemberReq.setToIds(req.getToIds());
                addFriendShipGroupMemberReq.setAppId(req.getAppId());
                imFriendshipGroupMemberService.addGroupMember(addFriendShipGroupMemberReq);
            }

        } catch (DuplicateKeyException e) {
            e.getStackTrace();
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }

        AddFriendGroupPack addFriendGropPack = new AddFriendGroupPack();
        addFriendGropPack.setFromId(req.getFromId());
        addFriendGropPack.setGroupName(req.getGroupName());
        addFriendGropPack.setSequence(seq);
        messageProducer.sendToUserExceptClient(req.getFromId(),
                FriendshipEventCommand.FRIEND_GROUP_ADD,
                addFriendGropPack, new ClientInfo(req.getAppId(),
                        req.getClientType(), req.getImei()));

        //写入seq
        writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendshipGroup, seq);

        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO deleteGroup(DeleteFriendShipGroupReq req) {

        for (String groupName : req.getGroupName()) {
            ImFriendshipGroup imFriendshipGroup = new ImFriendshipGroup();
            imFriendshipGroup.setFromId(req.getFromId());
            imFriendshipGroup.setGroupName(groupName);
            imFriendshipGroup.setAppId(req.getAppId());
            imFriendshipGroup.setDelFlag(DelFlagEnum.NORMAL.getCode());

            ImFriendshipGroup entity = imFriendshipGroupMapper.selectOne(imFriendshipGroup);

            if (entity != null) {
                long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
                ImFriendshipGroup update = new ImFriendshipGroup();
                BeanUtils.copyProperties(imFriendshipGroup, update);
                update.setGroupId(entity.getGroupId());
                update.setSequence(seq);
                update.setDelFlag(DelFlagEnum.DELETE.getCode());// 进行软删除
                imFriendshipGroupMemberService.clearGroupMember(entity.getGroupId().longValue());
                imFriendshipGroupMapper.updateByPrimaryKey(update);

                DeleteFriendGroupPack deleteFriendGroupPack = new DeleteFriendGroupPack();
                deleteFriendGroupPack.setFromId(req.getFromId());
                deleteFriendGroupPack.setGroupName(groupName);
                deleteFriendGroupPack.setSequence(seq);
                //TCP通知
                messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_DELETE,
                        deleteFriendGroupPack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
                //写入seq
                writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendshipGroup, seq);
            }
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO<ImFriendshipGroup> getGroup(String fromId, String groupName, Integer appId) {
        ImFriendshipGroup imFriendshipGroup = new ImFriendshipGroup();
        imFriendshipGroup.setFromId(fromId);
        imFriendshipGroup.setGroupName(groupName);
        imFriendshipGroup.setAppId(appId);
        imFriendshipGroup.setDelFlag(DelFlagEnum.NORMAL.getCode());

        ImFriendshipGroup entity = imFriendshipGroupMapper.selectOne(imFriendshipGroup);

        if (entity == null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    @Override
    public Long updateSeq(String fromId, String groupName, Integer appId) {

        ImFriendshipGroup imFriendshipGroup = new ImFriendshipGroup();
        imFriendshipGroup.setFromId(fromId);
        imFriendshipGroup.setGroupName(groupName);
        imFriendshipGroup.setAppId(appId);
        ImFriendshipGroup entity = imFriendshipGroupMapper.selectOne(imFriendshipGroup);

        long seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.FriendshipGroup);
        ImFriendshipGroup group = new ImFriendshipGroup();
        group.setGroupId(entity.getGroupId());
        group.setSequence(seq);
        imFriendshipGroupMapper.updateByPrimaryKey(group);
        return seq;

    }
}
