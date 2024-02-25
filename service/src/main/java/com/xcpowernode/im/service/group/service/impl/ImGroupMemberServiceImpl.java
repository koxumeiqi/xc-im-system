package com.xcpowernode.im.service.group.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xcpower.codec.pack.group.*;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.GroupErrorCode;
import com.xcpower.im.enums.GroupMemberRoleEnum;
import com.xcpower.im.enums.GroupStatusEnum;
import com.xcpower.im.enums.GroupTypeEnum;
import com.xcpower.im.enums.command.GroupEventCommand;
import com.xcpower.im.exception.ApplicationException;
import com.xcpower.im.model.ClientInfo;
import com.xcpowernode.im.service.config.AppConfig;
import com.xcpowernode.im.service.group.dao.ImGroupMapper;
import com.xcpowernode.im.service.group.dao.ImGroupMemberMapper;
import com.xcpowernode.im.service.group.entity.ImGroup;
import com.xcpowernode.im.service.group.entity.ImGroupKey;
import com.xcpowernode.im.service.group.entity.ImGroupMember;
import com.xcpowernode.im.service.group.model.callback.AddMemberAfterCallback;
import com.xcpowernode.im.service.group.model.callback.DestroyGroupCallbackDto;
import com.xcpowernode.im.service.group.model.req.*;
import com.xcpowernode.im.service.group.model.resp.AddMemberResp;
import com.xcpowernode.im.service.group.model.resp.GetRoleInGroupResp;
import com.xcpowernode.im.service.group.service.ImGroupMemberService;
import com.xcpowernode.im.service.group.service.ImGroupService;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import com.xcpowernode.im.service.user.service.ImUserService;
import com.xcpowernode.im.service.utils.CallbackService;
import com.xcpowernode.im.service.utils.GroupMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.*;
import java.util.List;


@Slf4j
@Service
public class ImGroupMemberServiceImpl implements ImGroupMemberService {

    @Autowired
    private ImGroupMemberMapper imGroupMemberMapper;

    @Autowired
    private ImGroupService imGroupService;

    @Autowired
    private ImGroupMemberService thisService;// 控制事务

    @Autowired
    private ImUserService imUserService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CallbackService callbackService;

    @Autowired
    private GroupMessageProducer groupMessageProducer;

    @Override
    public ResponseVO importGroupMember(ImportGroupMemberReq req) {

        ResponseVO responseVO = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!responseVO.isOk()) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }

        List<AddMemberResp> resp = new ArrayList<>();// 响应集

        for (GroupMemberDto dto : req.getMembers()) {
            ResponseVO res = thisService.addGroupMember(req.getGroupId(), req.getAppId(), dto);
            AddMemberResp addMemberResp = new AddMemberResp();
            addMemberResp.setMemberId(dto.getMemberId());
            if (res.isOk()) {
                addMemberResp.setResult(0); // 成功
            } else if (GroupErrorCode.USER_IS_JOINED_GROUP.getCode() == res.getCode()) {
                addMemberResp.setResult(2);// 已是群成员
            } else {
                addMemberResp.setResult(1);// 失败
            }
            resp.add(addMemberResp);
        }
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO addMember(AddGroupMemberReq req) {

        List<AddMemberResp> resp = new ArrayList<>();

        boolean isAdmin = false;
        ResponseVO groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        List<GroupMemberDto> memberDtos = req.getMembers();

        // 利用回调除去不准进群的成员
        if (appConfig.isAddGroupMemberBeforeCallback()) {
            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.AddFriendBefore,
                    JSONObject.toJSONString(req));
            if (!responseVO.isOk()) {
                return responseVO;
            }
            try {
                memberDtos =
                        JSONArray.parseArray(JSONObject.toJSONString(responseVO.getData()), GroupMemberDto.class);

            } catch (Exception e) {
                e.printStackTrace();
                log.error("GroupMemberAddBefore 回调失败：{}", req.getAppId());

            }

        }

        /**
         * 私有群（private）	类似普通微信群，创建后仅支持已在群内的好友邀请加群，且无需被邀请方同意或群主审批
         * 公开群（Public）	类似 QQ 群，创建后群主可以指定群管理员，需要群主或管理员审批通过才能入群
         * 群类型 1私有群（类似微信） 2公开群(类似qq）
         *
         */
        // 获取执行者的身份
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = thisService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        GetRoleInGroupResp roleData = roleInGroupOne.getData();
        // 判读是否拥有管理员或者群主的权限
        isAdmin = roleData.getRole() == GroupMemberRoleEnum.MAMAGER.getCode() || roleData.getRole() == GroupMemberRoleEnum.OWNER.getCode();

        ImGroup group = (ImGroup) groupResp.getData();
        if (!isAdmin && GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
        }

        List<String> successId = new ArrayList<>();
        for (GroupMemberDto memberDto : memberDtos) {
            ResponseVO responseVO = null;
            try {
                responseVO = thisService.addGroupMember(req.getGroupId(), req.getAppId(), memberDto);
            } catch (Exception e) {
                e.printStackTrace();
                responseVO = ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
            }
            AddMemberResp addMemberResp = new AddMemberResp();
            addMemberResp.setMemberId(memberDto.getMemberId());
            if (responseVO.isOk()) {
                successId.add(memberDto.getMemberId());
                addMemberResp.setResult(0);
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(2);
                addMemberResp.setResultMessage(responseVO.getMsg());
            } else {
                addMemberResp.setResult(1);
                addMemberResp.setResultMessage(responseVO.getMsg());
            }
            resp.add(addMemberResp);
        }

        // TCP 通知
        AddGroupMemberPack addGroupMemberPack = new AddGroupMemberPack();
        addGroupMemberPack.setGroupId(req.getGroupId());
        addGroupMemberPack.setMembers(successId);
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.ADDED_MEMBER, addGroupMemberPack
                , new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        if (appConfig.isModifyGroupAfterCallback()) {
            AddMemberAfterCallback dtoCallBack = new AddMemberAfterCallback();
            dtoCallBack.setGroupId(req.getGroupId());
            dtoCallBack.setMemberId(resp);
            dtoCallBack.setOperater(req.getOperator());
            dtoCallBack.setGroupType(group.getGroupType());

            callbackService.callback(req.getAppId(),
                    Constants.CallbackCommand.GroupMemberAddAfter,
                    JSONObject.toJSONString(dtoCallBack));
        }

        return ResponseVO.successResponse(resp);
    }

    @Override
    @Transactional
    public ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto) {

        ImGroupMember imGroupMember = new ImGroupMember();
        imGroupMember.setGroupId(groupId);
        imGroupMember.setAppId(appId);
        imGroupMember.setRole(GroupMemberRoleEnum.OWNER.getCode());
        int res1 = imGroupMemberMapper.selectCount(imGroupMember);
        // 判断需要导入的成员是否是群主
        if (res1 > 0 && dto.getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_HAVE_OWNER);
        }

        // 不是群主的话开始导入成员
        long now = System.currentTimeMillis();
        imGroupMember.setRole(null);// 避免动态查询条件
        imGroupMember.setMemberId(dto.getMemberId());
        ImGroupMember res2 = imGroupMemberMapper.selectOne(imGroupMember);
        if (res2 == null) {// 说明第一次加入该群
            ImGroupMember insert = new ImGroupMember();
            BeanUtils.copyProperties(dto, insert);
            insert.setJoinTime(now);
            insert.setGroupId(groupId);
            insert.setAppId(appId);
            insert.setJoinTime(now);
            int insertRes = imGroupMemberMapper.insertSelective(insert);
            if (insertRes == 1) {
                return ResponseVO.successResponse();
            }
        } else if (res2.getRole() == GroupMemberRoleEnum.LEAVE.getCode()) {// 说明是再次入群的，更新软删除字段即可
            ImGroupMember update = new ImGroupMember();
            BeanUtils.copyProperties(dto, update);
            update.setJoinTime(now);
            update.setGroupId(groupId);
            update.setAppId(appId);
            int updateRes = imGroupMemberMapper.updateByPrimaryKeySelective(update);
            if (updateRes == 1) {
                return ResponseVO.successResponse();
            }
        }
        return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
    }

    /**
     * 移除群成员；私有群只能群主移除，公开群的话群主可以移除任何人（除了自己），管理员可以移除普通成员
     *
     * @param req
     * @return
     */
    @Override
    public ResponseVO removeMember(RemoveGroupMemberReq req) {

        // 判断这个群是否存在
        ResponseVO groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        ImGroup group = (ImGroup) groupResp.getData();

        boolean isAdmin = false;

        // 获取执行者的身份
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = thisService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        GetRoleInGroupResp roleData = roleInGroupOne.getData();
        Integer role = roleData.getRole();

        boolean isOwner = role == GroupMemberRoleEnum.OWNER.getCode();
        boolean isManager = role == GroupMemberRoleEnum.MAMAGER.getCode();

        // 先排除掉不能删的情况，然后再进行删除

        if (!isOwner && !isManager) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
        }

        // 私有群必须是群主才能踢人
        if (!isOwner && GroupTypeEnum.PRIVATE.getCode() == group.getGroupType()) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
        }

        // 公开群管理员和群主可踢人，但管理员只能踢普通群成员
        if (GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
            //获取被踢人的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupResp = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupResp.isOk()) {
                return roleInGroupResp;
            }
            GetRoleInGroupResp memberData = roleInGroupResp.getData();
            Integer memberRole = memberData.getRole();
            // 群主是无法被踢掉的
            if (memberRole == GroupMemberRoleEnum.OWNER.getCode()) {
                throw new ApplicationException(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
            }
            // 是管理员并且被踢人不是群成员，无法操作
            if (isManager && memberRole != GroupMemberRoleEnum.ORDINARY.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }
        ResponseVO removeResponseVO = thisService.removeGroupMember(req.getGroupId(), req.getAppId(), req.getMemberId());

        if (removeResponseVO.isOk()) {

            RemoveGroupMemberPack removeGroupMemberPack = new RemoveGroupMemberPack();
            removeGroupMemberPack.setGroupId(req.getGroupId());
            removeGroupMemberPack.setMember(req.getMemberId());
            groupMessageProducer.producer(req.getMemberId(), GroupEventCommand.DELETED_MEMBER, removeGroupMemberPack
                    , new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

            if (appConfig.isDeleteGroupMemberAfterCallback()) {
                callbackService.callback(req.getAppId(),
                        Constants.CallbackCommand.GroupMemberDeleteAfter,
                        JSONObject.toJSONString(req));
            }
        }

        return removeResponseVO;
    }

    /**
     * 供内部使用的删除群成员
     *
     * @param groupId
     * @param appId
     * @param memberId
     * @return
     */
    @Override
    @Transactional
    public ResponseVO removeGroupMember(String groupId, Integer appId, String memberId) {

        // 先判断用户是否存在
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(memberId, appId);
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }
        // 然后获取用户角色信息，准备更新role状态
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(groupId, memberId, appId);
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        GetRoleInGroupResp data = roleInGroupOne.getData();
        ImGroupMember imGroupMember = new ImGroupMember();
        imGroupMember.setRole(GroupMemberRoleEnum.LEAVE.getCode());
        imGroupMember.setLeaveTime(System.currentTimeMillis());
        imGroupMember.setGroupMemberId(data.getGroupMemberId());
        imGroupMember.setAppId(appId);
        imGroupMemberMapper.updateByPrimaryKeySelective(imGroupMember);

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId) {

        // 判断群是否存在
        ResponseVO group = imGroupService.getGroup(groupId, appId);
        if (!group.isOk()) {
            return group;
        }

        ImGroupMember imGroupMember = new ImGroupMember();
        imGroupMember.setGroupId(groupId);
        imGroupMember.setMemberId(memberId);
        imGroupMember.setAppId(appId);
        ImGroupMember res = imGroupMemberMapper.selectOne(imGroupMember);
        if (res == null || res.getRole() == GroupMemberRoleEnum.LEAVE.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
        }
        GetRoleInGroupResp resp = new GetRoleInGroupResp();
        resp.setSpeakDate(res.getSpeakDate());
        resp.setGroupMemberId(res.getGroupMemberId());
        resp.setMemberId(res.getMemberId());
        resp.setRole(res.getRole());
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId) {
        List<ImGroupMember> imGroupMembers = imGroupMemberMapper.selectAllGroupMember(groupId, appId);
        return ResponseVO.successResponse(imGroupMembers);
    }

    /**
     * @param req
     * @return
     */
    @Override
    public ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req) {
        // 分页
        if (req.getLimit() != null) {
            PageHelper.startPage(req.getOffset(), req.getLimit());
            List<ImGroupMember> imGroupMembers = imGroupMemberMapper.selectJoinedGroup(req.getMemberId(), req.getAppId());
            PageInfo<ImGroupMember> pageInfo = new PageInfo<>(imGroupMembers);

            List<ImGroupMember> records = pageInfo.getList();
            Set<String> groupIds = new HashSet<>(); // 将群id封装起来
            records.forEach(e -> {
                groupIds.add(e.getGroupId());
            });
            return ResponseVO.successResponse(groupIds);
        }
        // 非分页
        return ResponseVO.successResponse(imGroupMemberMapper.getJoinedGroupId(req.getAppId(), req.getMemberId()));
    }

    /**
     * 判断用户是否存在群中
     *
     * @param groupId
     * @param memberId
     * @param appId
     * @return
     */
    @Override
    public boolean isGroupMember(String groupId, String memberId, Integer appId) {

        ImGroupMember imGroupMember = new ImGroupMember();
        imGroupMember.setMemberId(memberId);
        imGroupMember.setGroupId(groupId);
        imGroupMember.setAppId(appId);
        ImGroupMember res = imGroupMemberMapper.selectOne(imGroupMember);
        if (res == null) {  // 用户不在这个群中
            return false;
        }
        return true;
    }

    /**
     * 转发群主后的角色修改
     *
     * @param owner
     * @param groupId
     * @param appId
     * @return
     */
    @Transactional
    @Override
    public ResponseVO transferGroupMember(String owner, String groupId, Integer appId) {
        imGroupMemberMapper.updateOwnerRole(GroupMemberRoleEnum.OWNER.getCode(), GroupMemberRoleEnum.ORDINARY.getCode(), appId, groupId);
        imGroupMemberMapper.updateMemberRole(GroupMemberRoleEnum.OWNER.getCode(), appId, owner, groupId);
        return ResponseVO.successResponse();
    }

    /**
     * 退出群聊
     *
     * @param req
     * @return
     */
    @Transactional
    @Override
    public ResponseVO exitGroup(ExitGroupReq req) {

        // 先判断该用户是否存在
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(req.getOperator(), req.getAppId());
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }

        // 然后去获取其群-成员表中的主键ID
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = thisService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        // 如果是群主的话不能解散或者转移群主，不能退出群聊
        if (roleInGroupOne.getData().getRole() != null && roleInGroupOne.getData().getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_OWNER_IS_NOT_EXIT);
        }
        // 执行退出操作
        ImGroupMember imGroupMember = new ImGroupMember();
        imGroupMember.setGroupMemberId(roleInGroupOne.getData().getGroupMemberId());
        imGroupMember.setAppId(req.getAppId());
        imGroupMember.setRole(GroupMemberRoleEnum.LEAVE.getCode());
        int updateRes = imGroupMemberMapper.updateByPrimaryKeySelective(imGroupMember);
        if (updateRes != 1) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_EXIT_ERROR);
        }

        ExitGroupPack exitGroupPack = new ExitGroupPack();
        exitGroupPack.setUserId(req.getOperator());
        groupMessageProducer.producer(req.getOperator(),
                GroupEventCommand.EXIT_GROUP,
                exitGroupPack,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    /**
     * 修改群成员信息
     *
     * @param req
     * @return
     */
    @Transactional
    @Override
    public ResponseVO updateGroupMember(UpdateGroupMemberReq req) {


        // 判断是否存在该群
        ResponseVO group = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!group.isOk()) {
            return group;
        }

        ImGroup data = (ImGroup) group.getData();
        if (data.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        //是否是自己修改自己的资料
        boolean isMeOperate = req.getOperator().equals(req.getMemberId());
        boolean isAdmin = false;

        if (!isAdmin) {
            //昵称只能自己修改 权限只能群主或管理员修改
            if (!org.springframework.util.StringUtils.isEmpty(req.getAlias()) && !isMeOperate) {
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_ONESELF);
            }

            //私有群不能设置管理员
            if (data.getGroupType() == GroupTypeEnum.PRIVATE.getCode() &&
                    req.getRole() != null && (req.getRole() == GroupMemberRoleEnum.MAMAGER.getCode() ||
                    req.getRole() == GroupMemberRoleEnum.OWNER.getCode())) {
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
            // 如果要修改权限相关的则走下面的逻辑
            if (req.getRole() != null) {
                // 获取被操作人的是否在群内
                ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                if (!roleInGroupOne.isOk()) {
                    return roleInGroupOne;
                }

                //获取操作人权限
                ResponseVO<GetRoleInGroupResp> operateRoleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
                if (!operateRoleInGroupOne.isOk()) {
                    return operateRoleInGroupOne;
                }
                GetRoleInGroupResp resp = operateRoleInGroupOne.getData();
                Integer roleInfo = resp.getRole(); // 操作人的权限

                boolean isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
                boolean isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode();

                //不是管理员不能修改权限
                if (req.getRole() != null && !isOwner && !isManager) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

                //管理员只有群主能够设置
                if (req.getRole() != null && req.getRole() == GroupMemberRoleEnum.MAMAGER.getCode() && !isOwner) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }
            }
        }

        ImGroupMember update = new ImGroupMember();

        if (StringUtils.isNotBlank(req.getAlias())) {
            update.setAlias(req.getAlias());
        }

        //不能直接修改为群主
        if (req.getRole() != null && req.getRole() != GroupMemberRoleEnum.OWNER.getCode()) {
            update.setRole(req.getRole());
        }

        update.setExtra(req.getExtra());
        update.setGroupId(req.getGroupId());
        update.setMemberId(req.getMemberId());
        update.setAppId(req.getAppId());
        int updateRes = imGroupMemberMapper.updateGroupMemberInfo(update);
        if (updateRes != 1) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_UPDATE_MEMBER_ERROR);
        }

        UpdateGroupMemberPack pack = new UpdateGroupMemberPack();
        BeanUtils.copyProperties(req, pack);
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.UPDATED_MEMBER, pack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));


        return ResponseVO.successResponse();
    }

    /**
     * 按时限禁言群成员
     *
     * @param req
     * @return
     */
    @Override
    public ResponseVO speak(SpeakMemberReq req) {

        ResponseVO groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        boolean isAdmin = false;
        boolean isOwner = false;
        boolean isManager = false;
        GetRoleInGroupResp memberRole = null;

        if (!isAdmin) {

            // 获取操作人的权限  是管理员 or 群主 or 群成员
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = thisService.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }

            memberRole = roleInGroupOne.getData();
            Integer roleInfo = memberRole.getRole();

            isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
            isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode();

            // 不是管理员也不是群主的话无法禁言别人
            if (!isOwner && !isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

            // 获取被操作的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne2 = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne2.isOk()) {
                return roleInGroupOne2;
            }
            memberRole = roleInGroupOne2.getData();

            // 被操作人是群主只能app管理员操作
            if (memberRole.getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
            }

            // 操作人是管理员的话，只能禁言普通成员了
            if (isManager && memberRole.getRole() != GroupMemberRoleEnum.ORDINARY.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }

        }

        ImGroupMember imGroupMember = new ImGroupMember();
        if (memberRole == null) {
            // 获取被操作者的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            memberRole = roleInGroupOne.getData();
        }

        imGroupMember.setGroupMemberId(memberRole.getGroupMemberId());
        imGroupMember.setAppId(req.getAppId());
        if (req.getSpeakDate() > 0) {
            imGroupMember.setSpeakDate(System.currentTimeMillis() + req.getSpeakDate());
        } else {
            imGroupMember.setSpeakDate(req.getSpeakDate());
        }

        int updateRes = imGroupMemberMapper.updateByPrimaryKeySelective(imGroupMember);
        if (updateRes != 1) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_UPDATE_MEMBER_ERROR);
        }

        GroupMemberSpeakPack pack = new GroupMemberSpeakPack();
        BeanUtils.copyProperties(req, pack);
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.SPEAK_GOUP_MEMBER, pack,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    @Override
    public List<String> getGroupMemberId(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupMemberId(appId, groupId);
    }

    @Override
    public List<GroupMemberDto> getGroupManager(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupManager(groupId, appId);
    }

    @Override
    public ResponseVO<Collection<String>> syncMemberJoinedGroup(String operator, Integer appId) {
        return ResponseVO.successResponse(
                imGroupMemberMapper.syncJoinedGroupId(appId, operator,
                        GroupMemberRoleEnum.LEAVE.getCode()));
    }

}
