package com.xcpowernode.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.incrementer.IKeyGenerator;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xcpower.codec.pack.friendship.ApproverFriendRequestPack;
import com.xcpower.codec.pack.friendship.ReadAllFriendRequestPack;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.ApproverFriendRequestStatusEnum;
import com.xcpower.im.enums.FriendShipErrorCode;
import com.xcpower.im.enums.command.FriendshipEventCommand;
import com.xcpower.im.exception.ApplicationException;
import com.xcpowernode.im.service.friendship.dao.ImFriendshipRequestMapper;
import com.xcpowernode.im.service.friendship.entity.ImFriendshipRequest;
import com.xcpowernode.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.xcpowernode.im.service.friendship.model.req.FriendDto;
import com.xcpowernode.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.xcpowernode.im.service.friendship.service.ImFriendShipService;
import com.xcpowernode.im.service.friendship.service.ImFriendshipRequestService;
import com.xcpowernode.im.service.seq.RedisSeq;
import com.xcpowernode.im.service.utils.MessageProducer;
import com.xcpowernode.im.service.utils.WriteUserSeq;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.function.ToIntBiFunction;

@Service
public class ImFriendshipRequestServiceImpl implements ImFriendshipRequestService {

    @Autowired
    private ImFriendshipRequestMapper imFriendshipRequestMapper;

    @Autowired
    private ImFriendShipService imFriendShipService;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private RedisSeq redisSeq;

    @Autowired
    private WriteUserSeq writeUserSeq;


    @Transactional
    @Override
    public ResponseVO addFriendshipRequest(String fromId, FriendDto dto, Integer appId) {

        ImFriendshipRequest entity = imFriendshipRequestMapper.queryOneReqest(fromId, dto.getToId(), appId);

        long seq = redisSeq.doGetSeq(appId + ":" +
                Constants.SeqConstants.FriendshipRequest);

        if (entity == null) {
            // 插入
            entity = new ImFriendshipRequest();
            entity.setCreateTime(new Date().getTime());
            entity.setUpdateTime(new Date().getTime());
            entity.setAppId(appId);
            entity.setFromId(fromId);
            entity.setApproveStatus(0); // 设置批准为0
            entity.setReadStatus(0); // 设置是否已读为0
            entity.setSequence(seq);
            BeanUtils.copyProperties(dto, entity);
            int insertRes = imFriendshipRequestMapper.insertSelective(entity);
            if (insertRes != 1) {
                return ResponseVO.errorResponse();
            }
        } else {
            // 修改记录内容和更新时间
            if (StringUtils.isNotBlank(dto.getAddSource())) { // 来源
                entity.setAddWording(dto.getAddSource());
            }
            if (StringUtils.isNotBlank(dto.getRemark())) { // 备注
                entity.setRemark(dto.getRemark());
            }
            if (StringUtils.isNotBlank(dto.getAddWording())) { // 验证信息
                entity.setAddWording(dto.getAddWording());
            }
            entity.setSequence(seq);
            entity.setUpdateTime(new Date().getTime());
            int updateRes = imFriendshipRequestMapper.updateByPrimaryKeySelective(entity);
            if (updateRes != 1) {
                return ResponseVO.errorResponse();
            }
        }
        writeUserSeq.writeUserSeq(appId, dto.getToId(),
                Constants.SeqConstants.FriendshipRequest,
                seq);


        //发送好友申请的tcp给接收方
        messageProducer.sendToUser(dto.getToId(),
                null, "", FriendshipEventCommand.FRIEND_REQUEST,
                entity, appId);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO approverFriendRequest(ApproveFriendRequestReq req) {

        ImFriendshipRequest imFriendshipRequestEntity = imFriendshipRequestMapper.selectByPrimaryKey(req.getId().intValue());
        if (imFriendshipRequestEntity == null) {
            // 好友请求得存在
            throw new ApplicationException(FriendShipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
        }

        if (!req.getOperator().equals(imFriendshipRequestEntity.getToId())) {
            // 只能审批发给自己的好友请求
            throw new ApplicationException(FriendShipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
        }

        long seq = redisSeq.doGetSeq(req.getAppId() + ":" +
                Constants.SeqConstants.FriendshipRequest);

        // 已读申请好友列表
        ImFriendshipRequest update = new ImFriendshipRequest();
        update.setApproveStatus(req.getStatus());
        update.setUpdateTime(System.currentTimeMillis());
        update.setSequence(seq);

        update.setId(req.getId().intValue());
        int updateRes = imFriendshipRequestMapper.updateByPrimaryKeySelective(update);

        writeUserSeq.writeUserSeq(req.getAppId(), req.getOperator(),
                Constants.SeqConstants.FriendshipRequest,
                seq);

        // 判断是否是同意的好友申请，如果是同意的话就添加进好友列表
        if (ApproverFriendRequestStatusEnum.AGREE.getCode() == req.getStatus()) {
            // 同意的话就添加列表
            FriendDto dto = new FriendDto();
            dto.setToId(imFriendshipRequestEntity.getToId());
            dto.setAddWording(imFriendshipRequestEntity.getAddWording());
            dto.setRemark(imFriendshipRequestEntity.getRemark());
            dto.setAddSource(imFriendshipRequestEntity.getAddSource());
            imFriendShipService.doAddFriend(req, imFriendshipRequestEntity.getFromId(), dto, req.getAppId());
        }

        ApproverFriendRequestPack approverFriendRequestPack = new ApproverFriendRequestPack();
        approverFriendRequestPack.setId(req.getId());
        approverFriendRequestPack.setStatus(req.getStatus());
        messageProducer.sendToUser(imFriendshipRequestEntity.getToId(), req.getClientType(), req.getImei(), FriendshipEventCommand
                .FRIEND_REQUEST_APPROVER, approverFriendRequestPack, req.getAppId());

        return ResponseVO.successResponse();
    }

    /**
     * 获取好友申请列表集
     *
     * @param toId
     * @param appId
     * @return
     */
    @Override
    public ResponseVO getFriendRequest(String toId, Integer appId) {
        List<ImFriendshipRequest> imFriendshipRequests = imFriendshipRequestMapper.queryFriendRequestList(toId, appId);
        return ResponseVO.successResponse(imFriendshipRequests);
    }

    @Override
    public ResponseVO readFriendShipRequest(ReadFriendShipRequestReq req) {

        long seq = redisSeq.doGetSeq(req.getAppId() + ":" +
                Constants.SeqConstants.FriendshipRequest);

        ImFriendshipRequest update = new ImFriendshipRequest();
        update.setToId(req.getFromId());
        update.setAppId(req.getAppId());
        update.setReadStatus(1); // 设置为已读
        update.setSequence(seq);
        int updateCnt = imFriendshipRequestMapper.updateReadStatus(update);
        writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(),
                Constants.SeqConstants.FriendshipRequest,
                seq);

        //TCP通知
        ReadAllFriendRequestPack readAllFriendRequestPack = new ReadAllFriendRequestPack();
        readAllFriendRequestPack.setFromId(req.getFromId());
        readAllFriendRequestPack.setSequence(seq);
        messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(), FriendshipEventCommand
                .FRIEND_REQUEST_READ, readAllFriendRequestPack, req.getAppId());

        return ResponseVO.successResponse();
    }
}
