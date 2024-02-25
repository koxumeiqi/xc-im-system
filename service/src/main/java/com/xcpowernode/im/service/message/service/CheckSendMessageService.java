package com.xcpowernode.im.service.message.service;


import com.baomidou.mybatisplus.extension.api.R;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.enums.*;
import com.xcpowernode.im.service.config.AppConfig;
import com.xcpowernode.im.service.friendship.entity.ImFriendship;
import com.xcpowernode.im.service.friendship.model.req.GetRelationReq;
import com.xcpowernode.im.service.friendship.service.ImFriendShipService;
import com.xcpowernode.im.service.group.entity.ImGroup;
import com.xcpowernode.im.service.group.model.resp.GetRoleInGroupResp;
import com.xcpowernode.im.service.group.service.ImGroupMemberService;
import com.xcpowernode.im.service.group.service.ImGroupService;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import com.xcpowernode.im.service.user.service.ImUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckSendMessageService {

    @Autowired
    ImFriendShipService imFriendShipService;

    @Autowired
    ImUserService imUserService;

    @Autowired
    AppConfig appConfig;

    @Autowired
    ImGroupService imGroupService;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    /**
     * 校验发送方是否被禁言、禁用
     *
     * @param fromId
     * @param appId
     * @return
     */
    public ResponseVO checkSenderForbidAndMute(String fromId, Integer appId) {

        ResponseVO<ImUserDataEntity> userInfo = imUserService.getSingleUserInfo(fromId, appId);
        if (!userInfo.isOk()) {
            return userInfo;
        }
        ImUserDataEntity user = userInfo.getData();
        if (user.getForbiddenFlag() == UserForbiddenFlagEnum.FORBIBBEN.getCode()) {
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_FORBIBBEN);
        } else if (user.getSilentFlag() == UserSilentFlagEnum.MUTE.getCode()) {
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_MUTE);
        }

        return ResponseVO.successResponse();
    }

    /**
     * 校验是否是好友关系
     *
     * @param fromId
     * @param toId
     * @param appId
     * @return
     */
    public ResponseVO checkFriendShip(String fromId, String toId, Integer appId) {

        if (appConfig.isSendMessageCheckFriend()) {
            // 验证双方是否是好友
            GetRelationReq fromReq = new GetRelationReq();
            fromReq.setAppId(appId);
            fromReq.setFromId(fromId);
            fromReq.setToId(toId);
            ResponseVO<ImFriendship> fromRelationResp = imFriendShipService.getRelation(fromReq);
            if (!fromRelationResp.isOk()) {
                return fromRelationResp;
            }

            GetRelationReq toReq = new GetRelationReq();
            toReq.setAppId(appId);
            toReq.setFromId(toId);
            toReq.setToId(fromId);
            ResponseVO<ImFriendship> toRelationResp = imFriendShipService.getRelation(toReq);
            if (!toRelationResp.isOk()) {
                return toRelationResp;
            }

            if (appConfig.isSendMessageCheckBlack()) {
                if (FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != fromRelationResp.getData().getBlack()) {
                    return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
                }

                if (FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != toRelationResp.getData().getBlack()) {
                    return ResponseVO.errorResponse(FriendShipErrorCode.TARGET_IS_BLACK_YOU);
                }
            }

        }

        return ResponseVO.successResponse();


    }

    public ResponseVO checkGroupMessage(String fromId, String groupId, Integer appId) {

        ResponseVO responseVO = checkSenderForbidAndMute(fromId, appId);
        if (!responseVO.isOk()) {
            return responseVO;
        }

        // 判断群逻辑
        ResponseVO<ImGroup> group = imGroupService.getGroup(groupId, appId);
        if (!group.isOk()) {
            return group;
        }

        // 判断群成员是否在群内
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = imGroupMemberService.getRoleInGroupOne(groupId, fromId, appId);
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        GetRoleInGroupResp data = roleInGroupOne.getData();

        // 判断群是否禁言
        // 如果禁言 只有群管理和群主可以发言
        // 反过来就是说：如果群禁言了，普通成员的话不准发言
        ImGroup groupData = group.getData();
        if(groupData.getMute() == GroupMuteTypeEnum.MUTE.getCode()
        && data.getRole() == GroupMemberRoleEnum.ORDINARY.getCode()/*(data.getRole() != GroupMemberRoleEnum.MAMAGER.getCode() ||
                data.getRole() != GroupMemberRoleEnum.OWNER.getCode())*/){
            return ResponseVO.errorResponse(GroupErrorCode.THIS_GROUP_IS_MUTE);
        }

        // 如果是群员的话看看是不是被禁言了
        if(data.getSpeakDate() != null && data.getSpeakDate() > System.currentTimeMillis()){
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_MEMBER_IS_SPEAK);
        }

        return ResponseVO.successResponse();
    }


}
