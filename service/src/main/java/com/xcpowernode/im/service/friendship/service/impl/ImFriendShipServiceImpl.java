package com.xcpowernode.im.service.friendship.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xcpower.codec.pack.friendship.*;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.AllowFriendTypeEnum;
import com.xcpower.im.enums.CheckFriendShipTypeEnum;
import com.xcpower.im.enums.FriendShipErrorCode;
import com.xcpower.im.enums.FriendShipStatusEnum;
import com.xcpower.im.enums.command.FriendshipEventCommand;
import com.xcpower.im.model.RequestBase;
import com.xcpower.im.model.SyncReq;
import com.xcpower.im.model.SyncResp;
import com.xcpowernode.im.service.config.AppConfig;
import com.xcpowernode.im.service.friendship.dao.ImFriendshipMapper;
import com.xcpowernode.im.service.friendship.dao.ImFriendshipRequestMapper;
import com.xcpowernode.im.service.friendship.entity.ImFriendship;
import com.xcpowernode.im.service.friendship.entity.ImFriendshipKey;
import com.xcpowernode.im.service.friendship.model.callback.AddFriendAfterCallbackDto;
import com.xcpowernode.im.service.friendship.model.callback.AddFriendBlackAfterCallbackDto;
import com.xcpowernode.im.service.friendship.model.callback.DeleteFriendAfterCallbackDto;
import com.xcpowernode.im.service.friendship.model.req.*;
import com.xcpowernode.im.service.friendship.model.resp.CheckFriendShipResp;
import com.xcpowernode.im.service.friendship.model.resp.ImportFriendShipResp;
import com.xcpowernode.im.service.friendship.service.ImFriendShipService;
import com.xcpowernode.im.service.friendship.service.ImFriendshipRequestService;
import com.xcpowernode.im.service.seq.RedisSeq;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import com.xcpowernode.im.service.user.service.ImUserService;
import com.xcpowernode.im.service.utils.CallbackService;
import com.xcpowernode.im.service.utils.MessageProducer;
import com.xcpowernode.im.service.utils.WriteUserSeq;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ImFriendShipServiceImpl implements ImFriendShipService {

    @Autowired
    private ImFriendshipMapper imFriendshipMapper;

    @Autowired
    private ImUserService imUserService;

    @Autowired
    private ImFriendshipRequestService imFriendshipRequestService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CallbackService callbackService;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private WriteUserSeq writeUserSeq;

    @Autowired
    private RedisSeq redisSeq;


    /**
     * 导入用户现有数据
     *
     * @param req
     * @return
     */
    @Override
    public ResponseVO importFriendShip(ImportFriendShipReq req) {
        if (req.getFriendItem().size() > 100) {
            //TODO 超出长度
        }

        List<String> successIds = new ArrayList<>();
        List<String> errorIds = new ArrayList<>();
        // 遍历需要导入的用户
        for (ImportFriendShipReq.ImportFriendDto importFriendDto : req.getFriendItem()) {
            ImFriendship entity = new ImFriendship();
            // 属性填充
            BeanUtils.copyProperties(importFriendDto, entity);
            entity.setAppId(req.getAppId());
            entity.setFromId(req.getFromId());
            entity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            entity.setCreateTime(new Date().getTime());
            try {
                int insertRes = imFriendshipMapper.insertSelective(entity);
                if (insertRes == 1) {
                    successIds.add(entity.getToId());
                } else {
                    errorIds.add(entity.getToId());
                }
            } catch (Exception e) {
                errorIds.add(entity.getToId());
            }
        }
        ImportFriendShipResp resp = ImportFriendShipResp.builder()
                .successIds(successIds)
                .errorIds(errorIds)
                .build();
        return ResponseVO.successResponse(resp);
    }

    /**
     * 添加好友
     *
     * @param addFriendReq
     * @return
     */
    @Override
    public ResponseVO addFriend(AddFriendReq addFriendReq) {
        // 判断是否存在这俩用户
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(addFriendReq.getFromId(), addFriendReq.getAppId());
        if (!fromInfo.isOk()) {
            return fromInfo;
        }
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(addFriendReq.getToItem().getToId(), addFriendReq.getAppId());
        if (!toInfo.isOk()) {
            return toInfo;
        }

        if (appConfig.isAddFriendBeforeCallback()) {
            ResponseVO callbackResp = callbackService.beforeCallback(addFriendReq.getAppId(),
                    Constants.CallbackCommand.AddFriendBefore,
                    JSONObject.toJSONString(addFriendReq));
            if (!callbackResp.isOk()) {
                return callbackResp;
            }
        }

        ImUserDataEntity toUser = toInfo.getData();
        if (toUser.getFriendAllowType() != null &&
                toUser.getFriendAllowType() == AllowFriendTypeEnum.NOT_NEED.getCode()) {
            return doAddFriend(addFriendReq, addFriendReq.getFromId(), addFriendReq.getToItem(), addFriendReq.getAppId());
        }
        // 申请流程
        // 插入一条好友申请列表
        ImFriendshipKey imFriendshipKey = new ImFriendshipKey();
        imFriendshipKey.setAppId(addFriendReq.getAppId());
        imFriendshipKey.setToId(addFriendReq.getToItem().getToId());
        imFriendshipKey.setFromId(addFriendReq.getFromId());
        ImFriendship res = imFriendshipMapper.selectByPrimaryKey(imFriendshipKey);
        // 判断是否是好友，如果是好友的话就不必进行好友申请了
        if (res == null || res.getStatus() != FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
            ResponseVO responseVO = imFriendshipRequestService.addFriendshipRequest(addFriendReq.getFromId(), addFriendReq.getToItem(), addFriendReq.getAppId());
            if (!responseVO.isOk()) {
                return ResponseVO.errorResponse();
            }
        } else {
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
        }
        return ResponseVO.successResponse();
    }

    /**
     * 更新好友信息，比如更改了备注啊什么的
     *
     * @param updateFriendReq
     * @return
     */
    @Override
    public ResponseVO updateFriend(UpdateFriendReq updateFriendReq) {
        // 判断是否存在这俩用户
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(updateFriendReq.getFromId(), updateFriendReq.getAppId());
        if (!fromInfo.isOk()) {
            return fromInfo;
        }
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(updateFriendReq.getToItem().getToId(), updateFriendReq.getAppId());
        if (!toInfo.isOk()) {
            return toInfo;
        }

        ResponseVO responseVO = doUpdate(updateFriendReq.getFromId(),
                updateFriendReq.getToItem(),
                updateFriendReq.getAppId());

        if (responseVO.isOk()) {

            UpdateFriendPack updateFriendPack = new UpdateFriendPack();
            updateFriendPack.setRemark(updateFriendReq.getToItem().getRemark());
            updateFriendPack.setToId(updateFriendReq.getToItem().getToId());
            messageProducer.sendToUser(updateFriendReq.getFromId(),
                    updateFriendReq.getClientType(),
                    updateFriendReq.getImei(),
                    FriendshipEventCommand.FRIEND_UPDATE, updateFriendPack,
                    updateFriendReq.getAppId());

            if (appConfig.isModifyFriendAfterCallback()) {

                AddFriendAfterCallbackDto dtoCallBack = new AddFriendAfterCallbackDto();
                dtoCallBack.setFromId(updateFriendReq.getFromId());
                dtoCallBack.setToItem(updateFriendReq.getToItem());

                callbackService.beforeCallback(updateFriendReq.getAppId(),
                        Constants.CallbackCommand.UpdateFriendAfter,
                        JSONObject.toJSONString(dtoCallBack));
            }
        }
        return responseVO;
    }

    /**
     * 删除单个好友
     *
     * @param deleteFriendReq
     * @return
     */
    @Override
    public ResponseVO deleteFriend(DeleteFriendReq deleteFriendReq) {

        long seq = redisSeq.doGetSeq(deleteFriendReq.getAppId() + ":"
                + Constants.SeqConstants.Friendship);

        ImFriendshipKey imFriendshipKey = new ImFriendshipKey();
        BeanUtils.copyProperties(deleteFriendReq, imFriendshipKey);
        imFriendshipKey.setAppId(deleteFriendReq.getAppId());
        ImFriendship entity = imFriendshipMapper.selectByPrimaryKey(imFriendshipKey);
        // 判断是不是已经是好友，是好友就改成删除状态
        if (entity != null) {
            if (entity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                ImFriendship imFriendship = new ImFriendship();
                imFriendship.setStatus(FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
                BeanUtils.copyProperties(deleteFriendReq, imFriendship);
                imFriendship.setFriendSequence(seq);
                int updateCnt = imFriendshipMapper.updateByPrimaryKeySelective(imFriendship);
                if (updateCnt != 1) {
                    return ResponseVO.errorResponse(FriendShipErrorCode.DELETE_FRIEND_ERROR);
                }
                writeUserSeq.writeUserSeq(deleteFriendReq.getAppId(),
                        deleteFriendReq.getFromId(),
                        Constants.SeqConstants.Friendship,
                        seq);

                DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
                deleteFriendPack.setFromId(deleteFriendReq.getFromId());
                deleteFriendPack.setToId(deleteFriendReq.getToId());
                deleteFriendPack.setSequence(seq);
                messageProducer.sendToUser(deleteFriendReq.getFromId(),
                        deleteFriendReq.getClientType(),
                        deleteFriendReq.getImei(),
                        FriendshipEventCommand.FRIEND_DELETE,
                        deleteFriendPack,
                        deleteFriendReq.getAppId());

                if (appConfig.isDeleteFriendAfterCallback()) {

                    DeleteFriendAfterCallbackDto dtoCallBack = new DeleteFriendAfterCallbackDto();
                    dtoCallBack.setFromId(deleteFriendReq.getFromId());
                    dtoCallBack.setToId(deleteFriendReq.getToId());

                    callbackService.beforeCallback(deleteFriendReq.getAppId(),
                            Constants.CallbackCommand.DeleteFriendAfter,
                            JSONObject.toJSONString(dtoCallBack));
                }
                return ResponseVO.successResponse();

            } else {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }
        }

        return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
    }

    @Override
    public ResponseVO deleteAllFriend(DeleteFriendReq req) {
        ImFriendship imFriendship = new ImFriendship();
        imFriendship.setFromId(req.getFromId());
        imFriendship.setAppId(req.getAppId());
        imFriendship.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
        // 将好友之间的状态改为已删除状态
        int deleteRes = imFriendshipMapper.deleteAll(imFriendship, FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
        if (deleteRes > 0) {
            DeleteAllFriendPack deleteFriendPack = new DeleteAllFriendPack();
            deleteFriendPack.setFromId(req.getFromId());
            messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(), FriendshipEventCommand.FRIEND_ALL_DELETE,
                    deleteFriendPack, req.getAppId());
            return ResponseVO.successResponse();
        }
        return ResponseVO.errorResponse(FriendShipErrorCode.DELETE_FRIEND_ERROR);
    }

    /**
     * 获取有关fromId的所有好友链
     *
     * @param req
     * @return
     */
    @Override
    public ResponseVO getAllFriendShip(GetAllFriendShipReq req) {
        ImFriendship imFriendship = new ImFriendship();
        BeanUtils.copyProperties(req, imFriendship);
        imFriendship.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
        return ResponseVO.successResponse(imFriendshipMapper.queryAllNormalFriend(imFriendship));
    }

    /**
     * 获取好友链
     *
     * @param req
     * @return
     */
    @Override
    public ResponseVO getRelation(GetRelationReq req) {
        ImFriendship imFriendship = new ImFriendship();
        BeanUtils.copyProperties(req, imFriendship);
        imFriendship.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
        ImFriendship entity = imFriendshipMapper.queryOneNormalFriend(imFriendship);
        if (entity == null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.REPEATSHIP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    @Transactional
    public ResponseVO doUpdate(String fromId, FriendDto dto, Integer appId) {

        long seq = redisSeq.doGetSeq(appId + ":" +
                Constants.SeqConstants.Friendship);
        ImFriendship updateFriend = new ImFriendship();
        updateFriend.setExtra(dto.getExtra());
        updateFriend.setAddSource(dto.getAddSource());
        updateFriend.setRemark(dto.getRemark());
        updateFriend.setFromId(fromId);
        updateFriend.setAppId(appId);
        updateFriend.setToId(dto.getToId());
        updateFriend.setFriendSequence(seq);
        int updateRes = imFriendshipMapper.updateByPrimaryKeySelective(updateFriend);
        if (updateRes != 1) {
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_UPDATE_ERROR);
        }

        writeUserSeq.writeUserSeq(appId, fromId, Constants.SeqConstants.Friendship,
                seq);
        return ResponseVO.successResponse("修改成功");
    }

    @Transactional
    @Override
    public ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId) {
        // A-B
        // friend 表插入 A 和 B两条记录
        // 查询是否有记录存在，如果存在则判断状态，如果是已添加，则提示添加，如果是未添加，则修改状态
        ImFriendshipKey imFriendshipKey = new ImFriendshipKey();
        imFriendshipKey.setAppId(appId);
        imFriendshipKey.setFromId(fromId);
        imFriendshipKey.setToId(dto.getToId());
        ImFriendship entity = imFriendshipMapper.selectByPrimaryKey(imFriendshipKey);

        long seq = 0L;
        if (entity == null) {
            // 走添加逻辑
            entity = new ImFriendship();
            entity.setFromId(fromId);
            seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);
            entity.setAppId(appId);
            entity.setFriendSequence(seq);
            BeanUtils.copyProperties(dto, entity);
            entity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            entity.setCreateTime(new Date().getTime());
            int insertCnt = imFriendshipMapper.insert(entity);
            if (insertCnt != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            writeUserSeq.writeUserSeq(appId, fromId, Constants.SeqConstants.Friendship, seq);
        } else {
            // 存在判断状态
            if (entity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            } else {
                ImFriendship update = new ImFriendship();

                if (StringUtils.isNotBlank(dto.getAddSource())) {
                    update.setAddSource(dto.getAddSource());
                }

                if (StringUtils.isNotBlank(dto.getRemark())) {
                    update.setRemark(dto.getRemark());
                }

                if (StringUtils.isNotBlank(dto.getExtra())) {
                    update.setExtra(dto.getExtra());
                }
                seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);
                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                int cnt = imFriendshipMapper.updateByPrimaryKey(update);
                if (cnt != 1) {
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
                }
                writeUserSeq.writeUserSeq(appId, fromId, Constants.SeqConstants.Friendship, seq);
            }
        }

        // 另一方好友也得存在
        ImFriendshipKey imFriendshipKey2 = new ImFriendshipKey();
        imFriendshipKey.setAppId(appId);
        imFriendshipKey.setFromId(dto.getToId());
        imFriendshipKey.setToId(fromId);
        ImFriendship toEntity = imFriendshipMapper.selectByPrimaryKey(imFriendshipKey2);
        if (toEntity == null) {
            // 走添加逻辑
            toEntity = new ImFriendship();
            toEntity.setFromId(dto.getToId());
            toEntity.setFriendSequence(seq);
            toEntity.setAppId(appId);
            toEntity.setToId(fromId);
            toEntity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            toEntity.setCreateTime(new Date().getTime());
            toEntity.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
            int insertCnt = imFriendshipMapper.insert(toEntity);
            if (insertCnt != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            writeUserSeq.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.Friendship, seq);
        } else {
            // 存在判断状态
            if (FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()
                    != toEntity.getStatus()) {
                ImFriendship update = new ImFriendship();
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                update.setFriendSequence(seq);
                int cnt = imFriendshipMapper.updateByPrimaryKey(update);
                if (cnt != 1) {
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
                }
                writeUserSeq.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.Friendship, seq);
            }
        }

        // 发送给from
        AddFriendPack addFriendPack = new AddFriendPack();
        BeanUtils.copyProperties(entity, addFriendPack);
        addFriendPack.setSequence(seq);
        if (requestBase != null) {
            messageProducer.sendToUser(fromId, requestBase.getClientType(),
                    requestBase.getImei(),
                    FriendshipEventCommand.FRIEND_ADD, addFriendPack, requestBase.getAppId());
        } else {
            messageProducer.sendToUser(fromId,
                    FriendshipEventCommand.FRIEND_ADD, addFriendPack, requestBase.getAppId());
        }

        // 发送给to
        AddFriendPack addFriendToPack = new AddFriendPack();
        BeanUtils.copyProperties(toEntity, addFriendPack);
        messageProducer.sendToUser(toEntity.getFromId(),
                FriendshipEventCommand.FRIEND_ADD, addFriendToPack, requestBase.getAppId());


        // 之后回调
        if (appConfig.isAddFriendAfterCallback()) {

            AddFriendAfterCallbackDto dtoCallBack = new AddFriendAfterCallbackDto();
            dtoCallBack.setFromId(fromId);
            dtoCallBack.setToItem(dto);

            callbackService.beforeCallback(appId,
                    Constants.CallbackCommand.AddFriendAfter,
                    JSONObject.toJSONString(dtoCallBack));
        }


        return ResponseVO.successResponse("添加成功");
    }

    /**
     * 检查好友
     *
     * @param req
     * @return
     */
    @Override
    public ResponseVO chekFriendship(CheckFriendShipReq req) {

        Map<String, Integer> result
                = req.getToIds().stream()
                .collect(Collectors.toMap(Function.identity(), s -> 0));

        List<CheckFriendShipResp> checkFriendShipRespList = new ArrayList<>();
        // 如果是单向校验的话
        if (req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()) {
            checkFriendShipRespList = imFriendshipMapper.checkFriendShip(req);
            // checkFriendShipRespList.forEach(checkFriendShipResp -> checkFriendShipResp.setStatus(checkFriendShipResp.getStatus() == 1 ? 1 : 0));
        } else {
            checkFriendShipRespList = imFriendshipMapper.checkFriendShipBoth(req);
        }

        Map<String, Integer> collect = checkFriendShipRespList.stream()
                .collect(Collectors.toMap(CheckFriendShipResp::getToId
                        , CheckFriendShipResp::getStatus));

        // 验证双方没有加的情况下
        for (String toId : result.keySet()) {
            if (!collect.containsKey(toId)) {
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setStatus(result.get(toId));
                checkFriendShipRespList.add(checkFriendShipResp);
            }
        }

        return ResponseVO.successResponse(checkFriendShipRespList);
    }

    /**
     * 添加黑名单
     *
     * @param req
     * @return
     */
    @Transactional
    @Override
    public ResponseVO addBlack(AddFriendShipBlackReq req) {
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if (!fromInfo.isOk()) {
            return fromInfo;
        }
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToId(), req.getAppId());
        if (!toInfo.isOk()) {
            return toInfo;
        }
        // 添加黑名单逻辑
        ImFriendshipKey imFriendshipKey = new ImFriendshipKey();
        BeanUtils.copyProperties(req, imFriendshipKey);
        ImFriendship entity = imFriendshipMapper.selectByPrimaryKey(imFriendshipKey);
        Long seq = 0L;
        if (entity == null) {
            // 没好友也添加黑名单
            seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
            entity = new ImFriendship();
            entity.setFromId(req.getFromId());
            entity.setAppId(req.getAppId());
            entity.setToId(req.getToId());
            entity.setFriendSequence(seq);
            entity.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());
            int insertCnt = imFriendshipMapper.insert(entity);
            if (insertCnt != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_BLACK_ERROR);
            }
        } else {
            // 如果存在则判断状态，如果是拉黑，则提示已拉黑，如果是未拉黑，则修改状态
            if (entity.getBlack() != null && entity.getBlack() == FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode()) {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
            }
            entity.setFriendSequence(seq);
            entity.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());
            int updateRes = imFriendshipMapper.updateByPrimaryKeySelective(entity);
            if (updateRes != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_BLACK_ERROR);
            }
        }
        writeUserSeq.writeUserSeq(req.getAppId(),
                req.getFromId(),
                Constants.SeqConstants.Friendship,
                seq);

        AddFriendBlackPack addFriendBlackPack = new AddFriendBlackPack();
        addFriendBlackPack.setSequence(seq);
        addFriendBlackPack.setFromId(req.getFromId());
        addFriendBlackPack.setToId(req.getToId());
        //发送tcp通知
        messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(),
                FriendshipEventCommand.FRIEND_BLACK_ADD, addFriendBlackPack, req.getAppId());


        //之后回调
        if (appConfig.isAddFriendShipBlackAfterCallback()) {
            AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
            callbackDto.setFromId(req.getFromId());
            callbackDto.setToId(req.getToId());
            callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.AddBlackAfter, JSONObject
                            .toJSONString(callbackDto));
        }

        return ResponseVO.successResponse("添加黑名单成功");
    }

    /**
     * 移除好友黑名单
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseVO deleteBlack(DeleteBlackReq req) {
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if (!fromInfo.isOk()) {
            return fromInfo;
        }
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToId(), req.getAppId());
        if (!toInfo.isOk()) {
            return toInfo;
        }
        // 删除黑名单逻辑
        ImFriendshipKey imFriendshipKey = new ImFriendshipKey();
        BeanUtils.copyProperties(req, imFriendshipKey);
        ImFriendship entity = imFriendshipMapper.selectByPrimaryKey(imFriendshipKey);
        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
        // 判断是否存在这个好友关系
        if (entity != null) {
            if (entity.getBlack() != null && entity.getBlack() == FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode()) {
                ImFriendship imFriendship = new ImFriendship();
                imFriendship.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
                BeanUtils.copyProperties(req, imFriendship);
                imFriendship.setFriendSequence(seq);
                int updateCnt = imFriendshipMapper.updateByPrimaryKeySelective(imFriendship);

                if (updateCnt == 1) {
                    writeUserSeq.writeUserSeq(req.getAppId(),
                            req.getFromId(),
                            Constants.SeqConstants.Friendship, seq);

                    DeleteBlackPack deleteFriendPack = new DeleteBlackPack();
                    deleteFriendPack.setFromId(req.getFromId());
                    deleteFriendPack.setToId(req.getToId());
                    deleteFriendPack.setSequence(seq);
                    messageProducer.sendToUser(req.getFromId(),
                            req.getClientType(),
                            req.getImei(), FriendshipEventCommand.FRIEND_BLACK_DELETE,
                            deleteFriendPack, req.getAppId());

                    //之后回调
                    if (appConfig.isDeleteFriendShipBlackAfterCallback()) {
                        AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
                        callbackDto.setFromId(req.getFromId());
                        callbackDto.setToId(req.getToId());
                        callbackService.beforeCallback(req.getAppId(),
                                Constants.CallbackCommand.DeleteBlack, JSONObject
                                        .toJSONString(callbackDto));
                    }
                }

            }
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_NOT_YOUR_BLACK);
        }


        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO checkBlack(CheckFriendShipReq req) {

        Map<String, Integer> toIdMap
                = req.getToIds().stream().collect(Collectors
                .toMap(Function.identity(), s -> 0));

        List<CheckFriendShipResp> result;

        if (req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()) {
            result = imFriendshipMapper.checkFriendShipBlack(req);
        } else {
            result = imFriendshipMapper.checkFriendShipBlackBoth(req);
        }

        Map<String, Integer> collect = result.stream()
                .collect(Collectors
                        .toMap(CheckFriendShipResp::getToId,
                                CheckFriendShipResp::getStatus));
        for (String toId :
                toIdMap.keySet()) {
            if (!collect.containsKey(toId)) {
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setStatus(toIdMap.get(toId));
                result.add(checkFriendShipResp);
            }
        }

        return ResponseVO.successResponse(result);
    }

    @Override
    public ResponseVO syncFriendshipList(SyncReq req) {

        if (req.getMaxLimit() > 100) {
            req.setMaxLimit(100);
        }
        SyncResp<ImFriendship> resp = new SyncResp<>();
        List<ImFriendship> list = imFriendshipMapper.syncFriendshipList(req);
        if (!CollectionUtils.isEmpty(list)) {
            ImFriendship maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            // 设置最大seq
            Long friendShipMaxSeq = imFriendshipMapper.getFriendShipMaxSeq(req.getAppId(), req.getOperator());
            resp.setMaxSequence(friendShipMaxSeq);
            // 设置拉取完成
            resp.setCompleted(maxSeqEntity.getFriendSequence() >= friendShipMaxSeq);
        } else {
            resp.setCompleted(true);
        }

        return ResponseVO.successResponse(resp);
    }

    @Override
    public List<String> getAllFriendId(String userId, Integer appId) {
        return imFriendshipMapper.getAllFriendIds(userId, appId);
    }

}
