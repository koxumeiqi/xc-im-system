package com.xcpowernode.im.service.conversation.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.xcpower.codec.pack.conversation.DeleteConversationPack;
import com.xcpower.codec.pack.conversation.UpdateConversationPack;
import com.xcpower.im.ResponseVO;
import com.xcpower.im.constant.Constants;
import com.xcpower.im.enums.ConversationErrorCode;
import com.xcpower.im.enums.ConversationTypeEnum;
import com.xcpower.im.enums.command.ConversationEventCommand;
import com.xcpower.im.model.ClientInfo;
import com.xcpower.im.model.SyncReq;
import com.xcpower.im.model.SyncResp;
import com.xcpower.im.model.message.MessageReadedContent;
import com.xcpowernode.im.service.config.AppConfig;
import com.xcpowernode.im.service.conversation.dao.ImConversationSetDao;
import com.xcpowernode.im.service.conversation.entity.ImConversationSet;
import com.xcpowernode.im.service.conversation.entity.ImConversationSetKey;
import com.xcpowernode.im.service.conversation.model.DeleteConversationReq;
import com.xcpowernode.im.service.conversation.model.UpdateConversationReq;
import com.xcpowernode.im.service.seq.RedisSeq;
import com.xcpowernode.im.service.utils.MessageProducer;
import com.xcpowernode.im.service.utils.WriteUserSeq;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {

    @Autowired
    ImConversationSetDao imConversationSetDao;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    AppConfig appConfig;

    @Autowired
    RedisSeq redisSeq;

    @Autowired
    WriteUserSeq writeUserSeq;

    public String convertConversationId(Integer type, String fromId, String toId) {
        return type + "_" + fromId + "_" + toId;
    }

    public void messageMarkRead(MessageReadedContent messageReadedContent) {

        String toId = messageReadedContent.getToId();
        if (messageReadedContent.getConversationType() == ConversationTypeEnum.GROUP.getCode()) {
            toId = messageReadedContent.getGroupId();
        }

        // 搜索对应的消息
        ImConversationSetKey key = new ImConversationSetKey();
        String id = convertConversationId(messageReadedContent.getConversationType(),
                messageReadedContent.getFromId(), toId);
        key.setConversationId(id);
        key.setAppId(messageReadedContent.getAppId());
        ImConversationSet imConversationSet = imConversationSetDao.selectByPrimaryKey(key);
        if (imConversationSet == null) {
            ImConversationSet entity = new ImConversationSet();

            long seq = redisSeq.doGetSeq(messageReadedContent.getAppId() + ":" + Constants.SeqConstants.Conversation);
            entity.setConversationId(id);
            BeanUtils.copyProperties(messageReadedContent, entity);
            entity.setSequence(seq);
            // 再次赋值 toId 是上面可能是群会话，使得toId改变了
            entity.setToId(toId);
            entity.setReadedSequence(messageReadedContent.getMessageSequence());
            imConversationSetDao.insert(entity);
            writeUserSeq.writeUserSeq(messageReadedContent.getAppId(),
                    messageReadedContent.getFromId(),
                    Constants.SeqConstants.Conversation,
                    seq);
        } else {
            imConversationSet.setReadedSequence(messageReadedContent.getMessageSequence());
            imConversationSetDao.readMark(imConversationSet);
        }


    }

    /**
     * 删除一个会话
     *
     * @param req
     * @return
     */
    public ResponseVO deleteConversation(DeleteConversationReq req) {

        if (appConfig.isDeleteConversationSyncMode()) {
            DeleteConversationPack pack = new DeleteConversationPack();
            pack.setConversationId(req.getConversationId());
            messageProducer.sendToUserExceptClient(req.getFromId(),
                    ConversationEventCommand.CONVERSATION_DELETE,
                    pack, new ClientInfo(req.getAppId(),
                            req.getClientType(),
                            req.getImei()));
        }

        return ResponseVO.successResponse();

    }

    /**
     * 置顶或免打扰
     *
     * @param req
     * @return
     */
    public ResponseVO updateConversation(UpdateConversationReq req) {

        // 判断是否是需要设置置顶或者免打扰
        if (req.getIsTop() == null && req.getIsMute() == null) {
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_UPDATE_PARAM_ERROR);
        }


        ImConversationSetKey key = new ImConversationSetKey();
        key.setAppId(req.getAppId());
        key.setConversationId(req.getConversationId());
        ImConversationSet entity = imConversationSetDao.selectByPrimaryKey(key);
        if (entity != null) {

            long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Conversation);

            if (req.getIsTop() != null) {
                entity.setIsTop(req.getIsTop());
            }
            if (req.getIsMute() != null) {
                entity.setIsMute(req.getIsMute());
            }
            entity.setSequence(seq);
            imConversationSetDao.updateConversation(entity);

            writeUserSeq.writeUserSeq(req.getAppId(),
                    req.getFromId(),
                    Constants.SeqConstants.Conversation, seq);

            UpdateConversationPack pack = new UpdateConversationPack();
            pack.setConversationId(req.getConversationId());
            pack.setIsMute(entity.getIsMute());
            pack.setIsTop(entity.getIsTop());
            pack.setSequence(seq);
            pack.setConversationType(entity.getConversationType());
            messageProducer.sendToUserExceptClient(req.getFromId(),
                    ConversationEventCommand.CONVERSATION_UPDATE,
                    pack,
                    new ClientInfo(req.getAppId(),
                            req.getClientType(),
                            req.getImei()));
        }

        return ResponseVO.successResponse();

    }


    public ResponseVO syncConversationSet(SyncReq req) {
        if (req.getMaxLimit() > 100) {
            req.setMaxLimit(100);
        }

        SyncResp<ImConversationSet> resp = new SyncResp<>();
        //seq > req.getseq limit maxLimit
        List<ImConversationSet> list = imConversationSetDao
                .syncConversationSet(req);

        if (!CollectionUtils.isEmpty(list)) {
            ImConversationSet maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            //设置最大seq
            Long friendShipMaxSeq = imConversationSetDao.geConversationSetMaxSeq(req.getAppId(), req.getOperator());
            resp.setMaxSequence(friendShipMaxSeq);
            //设置是否拉取完毕
            resp.setCompleted(maxSeqEntity.getSequence() >= friendShipMaxSeq);
            return ResponseVO.successResponse(resp);
        }

        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }
}
