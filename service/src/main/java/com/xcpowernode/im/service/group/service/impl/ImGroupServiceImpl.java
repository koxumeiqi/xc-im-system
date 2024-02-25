package com.xcpowernode.im.service.group.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xcpower.codec.pack.group.*;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.*;
import com.xcpower.im.enums.command.GroupEventCommand;
import com.xcpower.im.exception.ApplicationException;
import com.xcpower.im.model.ClientInfo;
import com.xcpower.im.model.SyncReq;
import com.xcpower.im.model.SyncResp;
import com.xcpowernode.im.service.config.AppConfig;
import com.xcpowernode.im.service.group.dao.ImGroupMapper;
import com.xcpowernode.im.service.group.dao.ImGroupMemberMapper;
import com.xcpowernode.im.service.group.entity.ImGroup;
import com.xcpowernode.im.service.group.entity.ImGroupKey;
import com.xcpowernode.im.service.group.entity.ImGroupMember;
import com.xcpowernode.im.service.group.model.callback.DestroyGroupCallbackDto;
import com.xcpowernode.im.service.group.model.req.*;
import com.xcpowernode.im.service.group.model.resp.GetGroupResp;
import com.xcpowernode.im.service.group.model.resp.GetJoinedGroupResp;
import com.xcpowernode.im.service.group.model.resp.GetRoleInGroupResp;
import com.xcpowernode.im.service.group.service.ImGroupMemberService;
import com.xcpowernode.im.service.group.service.ImGroupService;
import com.xcpowernode.im.service.seq.RedisSeq;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import com.xcpowernode.im.service.user.service.ImUserService;
import com.xcpowernode.im.service.utils.CallbackService;
import com.xcpowernode.im.service.utils.GroupMessageProducer;
import com.xcpowernode.im.service.utils.MessageProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


@Service
public class ImGroupServiceImpl implements ImGroupService {

    @Autowired
    private ImGroupMapper imGroupMapper;

    @Autowired
    private ImUserService imUserService;

    @Autowired
    private ImGroupMemberService imGroupMemberService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CallbackService callbackService;

    @Autowired
    private GroupMessageProducer groupMessageProducer;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private RedisSeq redisSeq;

    @Override
    public ResponseVO importGroup(ImportGroupReq req) {

        // 判断群主这个用户是否存在
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(req.getOwnerId(), req.getAppId());
        if (!singleUserInfo.isOk()) {
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }

        if (StringUtils.isNotBlank(req.getGroupId())) {
            ImGroupKey imGroupKey = new ImGroupKey();
            imGroupKey.setGroupId(req.getGroupId());
            imGroupKey.setAppId(req.getAppId());
            ImGroup res = imGroupMapper.selectByPrimaryKey(imGroupKey, GroupStatusEnum.NORMAL.getCode());
            if (res != null) {
                // 返回记录已经存在
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_EXIST);
            }
        } else {
            req.setGroupId(UUID.randomUUID().toString().replace("-", ""));
        }
        ImGroup imGroup = new ImGroup();
        BeanUtils.copyProperties(req, imGroup);

        if (req.getStatus() == null) {
            imGroup.setStatus(GroupStatusEnum.NORMAL.getCode());
        }

        if (req.getCreateTime() == null) {
            imGroup.setCreateTime(System.currentTimeMillis());
        }

        int insertRes = imGroupMapper.insertSelective(imGroup);

        if (insertRes != 1) {
            throw new ApplicationException(GroupErrorCode.IMPORT_GROUP_ERROR);
        }

        // 公开群插入群主
        if (req.getGroupType() != null && req.getGroupType() == GroupTypeEnum.PUBLIC.getCode()) {
            // 将群主插入到群中
            GroupMemberDto groupMemberDto = new GroupMemberDto();
            groupMemberDto.setMemberId(req.getOwnerId());
            groupMemberDto.setRole(GroupMemberRoleEnum.OWNER.getCode());
            groupMemberDto.setAlias(req.getOwnerId());
            groupMemberDto.setJoinTime(System.currentTimeMillis());
            imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), groupMemberDto);
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getGroup(String groupId, Integer appId) {

        ImGroupKey imGroupKey = new ImGroupKey();
        imGroupKey.setGroupId(groupId);
        imGroupKey.setAppId(appId);
        ImGroup entity = imGroupMapper.selectByPrimaryKey(imGroupKey, GroupStatusEnum.NORMAL.getCode());
        // 判断群是否存在
        if (entity == null) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    @Override
    public ResponseVO getGroup(GetGroupReq req) {

        // 组成员信息
        ResponseVO<List<GroupMemberDto>> groupMember = imGroupMemberService.getGroupMember(req.getGroupId(), req.getAppId());
        if (!groupMember.isOk()) {
            return groupMember;
        }
        GetGroupResp getGroupResp = new GetGroupResp();
        getGroupResp.setMemberList(groupMember.getData());
        // 组信息
        ResponseVO group = getGroup(req.getGroupId(), req.getAppId());
        if (group.isOk()) {
            BeanUtils.copyProperties(group.getData(), getGroupResp);
        }
        return ResponseVO.successResponse(getGroupResp);
    }

    /**
     * 获取加入的群列表
     *
     * @param req
     * @return
     */
    @Override
    public ResponseVO getJoinedGroup(GetJoinedGroupReq req) {

        ResponseVO<Collection<String>> memberJoinedGroup = imGroupMemberService.getMemberJoinedGroup(req);
        if (memberJoinedGroup.isOk()) { // 如果获取已加入的群id成功的话（分页）
            GetJoinedGroupResp resp = new GetJoinedGroupResp();

            // 判断是否有群
            if (CollectionUtils.isEmpty(memberJoinedGroup.getData())) {
                resp.setTotalCount(0);
                resp.setGroupList(new ArrayList<>());
                return ResponseVO.successResponse(resp);
            }

            List<ImGroup> imGroups = imGroupMapper.selectList(req.getAppId(), memberJoinedGroup.getData(), req.getGroupType());
            resp.setGroupList(imGroups);
            resp.setTotalCount(imGroups.size());
            return ResponseVO.successResponse(resp);
        }
        return memberJoinedGroup;
    }

    @Override
    @Transactional
    public ResponseVO createGroup(CreateGroupReq req) {

        boolean isAdmin = false;

        // 创建者为群主
        if (!isAdmin) {
            req.setOwnerId(req.getOperator());
        }

        if (org.springframework.util.StringUtils.isEmpty(req.getGroupId())) {
            req.setGroupId(UUID.randomUUID().toString().replace("-", ""));
        } else {
            ImGroupKey imGroupKey = new ImGroupKey();
            imGroupKey.setGroupId(req.getGroupId());
            imGroupKey.setAppId(req.getAppId());
            ImGroup imGroup = imGroupMapper.selectByPrimaryKey(imGroupKey, GroupStatusEnum.NORMAL.getCode());
            if (imGroup == null) {
                throw new ApplicationException(GroupErrorCode.GROUP_IS_EXIST);
            }
        }

        // 公开群需要指定群主
        if (req.getGroupType() == GroupTypeEnum.PUBLIC.getCode() && StringUtils.isBlank(req.getOwnerId())) {
            throw new ApplicationException(GroupErrorCode.PUBLIC_GROUP_MUST_HAVE_OWNER);
        }

        ImGroup imGroup = new ImGroup();

        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Group);

        imGroup.setCreateTime(System.currentTimeMillis());
        imGroup.setStatus(GroupStatusEnum.NORMAL.getCode());
        BeanUtils.copyProperties(req, imGroup);
        imGroup.setSequence(seq);
        int insertRes = imGroupMapper.insertSelective(imGroup);


        // 公开群插入群主
        if (req.getGroupType() != null && req.getGroupType() == GroupTypeEnum.PUBLIC.getCode()) {
            // 将群主插入到群中
            GroupMemberDto groupMemberDto = new GroupMemberDto();
            groupMemberDto.setMemberId(req.getOwnerId());
            groupMemberDto.setRole(GroupMemberRoleEnum.OWNER.getCode());
            groupMemberDto.setAlias(req.getOwnerId());
            groupMemberDto.setJoinTime(System.currentTimeMillis());
            imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), groupMemberDto);
        }

        //插入群成员
        for (GroupMemberDto dto : req.getMember()) {
            imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), dto);
        }


        if (appConfig.isCreateGroupAfterCallback()) {
            callbackService.callback(req.getAppId(),
                    Constants.CallbackCommand.CreateGroupAfter,
                    JSONObject.toJSONString(imGroup));
        }

        // TCP通知
        CreateGroupPack createGroupPack = new CreateGroupPack();
        BeanUtils.copyProperties(imGroup, createGroupPack);
        createGroupPack.setSequence(seq);
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.CREATED_GROUP, createGroupPack
                , new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO updateBaseGroupInfo(UpdateGroupReq req) {

        ImGroupKey imGroupKey = new ImGroupKey();
        imGroupKey.setGroupId(req.getGroupId());
        imGroupKey.setAppId(req.getAppId());
        ImGroup res = imGroupMapper.selectByPrimaryKey(imGroupKey, GroupStatusEnum.NORMAL.getCode());
        if (res == null) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }

        boolean isAdmin = false; // 判断是否是管理员，后续处理权限

        if (!isAdmin) {
            // 不是管理员的话
            // 校验权限
            ResponseVO<GetRoleInGroupResp> response = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
            if (!response.isOk()) {
                return response;
            }
            GetRoleInGroupResp roleData = response.getData();
            int role = roleData.getRole();

            boolean isManager = role == GroupMemberRoleEnum.MAMAGER.getCode() || role == GroupMemberRoleEnum.OWNER.getCode();

            // 公开群只能群主和管理员可以修改资料
            if (!isManager && GroupTypeEnum.PUBLIC.getCode() == res.getGroupType()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

        }

        ImGroup update = new ImGroup();

        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Group);

        BeanUtils.copyProperties(req, update);
        update.setSequence(seq);
        update.setUpdateTime(System.currentTimeMillis());
        int updateRes = imGroupMapper.updateByPrimaryKeySelective(update);
        if (updateRes != 1) {
            throw new ApplicationException(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
        }

        if (appConfig.isModifyGroupAfterCallback()) {
            callbackService.callback(req.getAppId(),
                    Constants.CallbackCommand.UpdateGroupAfter,
                    JSONObject.toJSONString(imGroupMapper.selectByPrimaryKey(imGroupKey, GroupStatusEnum.NORMAL.getCode())));
        }
        // TCP 通知
        UpdateGroupInfoPack pack = new UpdateGroupInfoPack();
        BeanUtils.copyProperties(req, pack);
        pack.setSequence(seq);
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.UPDATED_GROUP,
                pack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    /**
     * 仅能通过群主解散
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO destroyGroup(DestroyGroupReq req) {

        // 判断群是否存在
        ImGroupKey imGroupKey = new ImGroupKey();
        imGroupKey.setAppId(req.getAppId());
        imGroupKey.setGroupId(req.getGroupId());
        ImGroup imGroup = imGroupMapper.selectByPrimaryKey(imGroupKey, null);
        if (imGroup == null) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }

        if (imGroup.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }
        // 判断该用户是否有解散群主的权限，只有群主才能解散
        boolean isAdmin = false;
        if (!isAdmin) {

            if (imGroup.getGroupType() == GroupTypeEnum.PRIVATE.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }

            if (imGroup.getGroupType() == GroupTypeEnum.PUBLIC.getCode() &&
                    !imGroup.getOwnerId().equals(req.getOperator())) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }
        // 解散群
        ImGroup update = new ImGroup();

        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Group);
        update.setSequence(seq);

        update.setStatus(GroupStatusEnum.DESTROY.getCode());
        update.setAppId(req.getAppId());
        update.setGroupId(req.getGroupId());
        int updateRes = imGroupMapper.updateByPrimaryKeySelective(update);
        if (updateRes != 1) {
            throw new ApplicationException(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
        }

        if (appConfig.isModifyGroupAfterCallback()) {

            DestroyGroupCallbackDto dtoCallBack = new DestroyGroupCallbackDto();
            dtoCallBack.setGroupId(req.getGroupId());

            callbackService.callback(req.getAppId(),
                    Constants.CallbackCommand.DestoryGroupAfter,
                    JSONObject.toJSONString(dtoCallBack));
        }

        // TCP 通知
        DestroyGroupPack pack = new DestroyGroupPack();
        pack.setGroupId(req.getGroupId());
        pack.setSequence(seq);
        groupMessageProducer.producer(req.getOperator(),
                GroupEventCommand.DESTROY_GROUP, pack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    /**
     * 群的禁言管理
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO muteGroup(MuteGroupReq req) {

        // 判断群是否存在
        ResponseVO<ImGroup> groupResp = getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        if (groupResp.getData().getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        boolean isAdmin = false;
        if (!isAdmin) {
            // 不是后台调用需要检查权限
            ResponseVO<GetRoleInGroupResp> role = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());

            if (!role.isOk()) {
                return role;
            }

            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            boolean isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode() || roleInfo == GroupMemberRoleEnum.OWNER.getCode();

            //公开群只能群主修改资料
            if (!isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

        }

        // 开始禁言相关的修改操作
        ImGroup update = new ImGroup();
        update.setMute(req.getMute());
        update.setAppId(req.getAppId());
        update.setGroupId(req.getGroupId());
        int updateRes = imGroupMapper.updateByPrimaryKeySelective(update);
        if (updateRes != 1) {
            return ResponseVO.errorResponse(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
        }

        // TCP 通知
        MuteGroupPack muteGroupPack = new MuteGroupPack();
        muteGroupPack.setGroupId(req.getGroupId());
        messageProducer.sendToUser(req.getGroupId(),
                GroupEventCommand.MUTE_GROUP,
                muteGroupPack,
                req.getAppId());


        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO transferGroup(TransferGroupReq req) {

        // 判断执行人权限是否是群主
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }

        if (roleInGroupOne.getData().getRole() != GroupMemberRoleEnum.OWNER.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
        }

        // 判断群是否存在
        ImGroupKey imGroupKey = new ImGroupKey();
        imGroupKey.setAppId(req.getAppId());
        imGroupKey.setGroupId(req.getGroupId());
        ImGroup imGroup = imGroupMapper.selectByPrimaryKey(imGroupKey, null);
        if (imGroup == null) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }

        if (imGroup.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        // 判断准备转让者是否是群里的成员
        boolean isGroupMember = imGroupMemberService.isGroupMember(req.getGroupId(), req.getOperator(), req.getAppId());
        if (!isGroupMember) {
            throw new ApplicationException(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
        }

        // 转让
        ImGroup update = new ImGroup();
        update.setOwnerId(req.getOwnerId());
        update.setAppId(req.getAppId());
        update.setGroupId(req.getGroupId());
        imGroupMapper.updateByPrimaryKeySelective(update);
        imGroupMemberService.transferGroupMember(req.getOwnerId(), req.getGroupId(), req.getAppId());

        // TCP 通知
        TransferGroupPack transferGroupPack = new TransferGroupPack();
        transferGroupPack.setGroupId(req.getGroupId());
        transferGroupPack.setOwnerId(req.getOwnerId()); // 现在的群主
        messageProducer.sendToUser(req.getOwnerId(),
                GroupEventCommand.TRANSFER_GROUP,
                transferGroupPack,
                req.getAppId());

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO syncJoinedGroupList(SyncReq req) {

        if (req.getMaxLimit() > 100) {
            req.setMaxLimit(100);
        }

        SyncResp<ImGroup> resp = new SyncResp<>();

        ResponseVO<Collection<String>> memberJoinedGroup = imGroupMemberService.syncMemberJoinedGroup(req.getOperator(), req.getAppId());
        if (memberJoinedGroup.isOk()) {

            Collection<String> data = memberJoinedGroup.getData();

            List<ImGroup> list = imGroupMapper.syncJoinedGroupList(req, data);

            if (!CollectionUtils.isEmpty(list)) {
                ImGroup maxSeqEntity
                        = list.get(list.size() - 1);
                resp.setDataList(list);
                //设置最大seq
                Long maxSeq =
                        imGroupMapper.getGroupMaxSeq(data, req.getAppId());
                resp.setMaxSequence(maxSeq);
                //设置是否拉取完毕
                resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
                return ResponseVO.successResponse(resp);
            }

        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);

    }

    @Override
    public Long getUserGroupMaxSeq(String userId, Integer appId) {
        ResponseVO<Collection<String>> memberJoinedGroup = imGroupMemberService.syncMemberJoinedGroup(userId, appId);
        if (!memberJoinedGroup.isOk()) {
            throw new ApplicationException(500, "");
        }
        Long maxSeq =
                imGroupMapper.getGroupMaxSeq(memberJoinedGroup.getData(),
                        appId);
        return maxSeq;
    }
}
