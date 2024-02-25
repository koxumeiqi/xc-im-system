package org.example.message.store.service;


import com.xcpower.im.model.message.GroupChatMessageContent;
import com.xcpower.im.model.message.MessageContent;
import org.example.message.store.dao.ImGroupMessageHistoryMapper;
import org.example.message.store.dao.ImMessageBodyMapper;
import org.example.message.store.dao.ImMessageHistoryMapper;
import org.example.message.store.entity.ImGroupMessageHistory;
import org.example.message.store.entity.ImMessageBody;
import org.example.message.store.entity.ImMessageHistory;
import org.example.message.store.model.DoStoreGroupMessageDto;
import org.example.message.store.model.DoStoreP2PMessageDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StoreMessageService {

    @Autowired
    ImMessageHistoryMapper imMessageHistoryMapper;

    @Autowired
    ImMessageBodyMapper imMessageBodyMapper;

    @Autowired
    ImGroupMessageHistoryMapper imGroupMessageHistoryMapper;

    @Transactional
    public void doStoreP2PMessage(DoStoreP2PMessageDto dto){
        imMessageBodyMapper.insertSelective(dto.getMessageBody());
        List<ImMessageHistory> imMessageHistories = extractToP2PMessageHistory(dto.getMessageContent(), dto.getMessageBody());
        imMessageHistoryMapper.insert(imMessageHistories);
    }

    public List<ImMessageHistory> extractToP2PMessageHistory(MessageContent messageContent,
                                                             ImMessageBody imMessageBody) {

        List<ImMessageHistory> list = new ArrayList<>();

        ImMessageHistory fromHistory = new ImMessageHistory();
        BeanUtils.copyProperties(messageContent,fromHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        fromHistory.setMessageKey(imMessageBody.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());
        fromHistory.setMessageTime(imMessageBody.getMessageTime());
        fromHistory.setSequence(messageContent.getMessageSequence());

        ImMessageHistory toHistory = new ImMessageHistory();
        BeanUtils.copyProperties(messageContent,toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBody.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());
        toHistory.setMessageTime(imMessageBody.getMessageTime());
        toHistory.setSequence(messageContent.getMessageSequence());

        list.add(fromHistory);
        list.add(toHistory);

        return list;
    }


    @Transactional
    public void doStoreGroupMessage(DoStoreGroupMessageDto dto) {
        imMessageBodyMapper.insertSelective(dto.getMessageBody());
        ImGroupMessageHistory imGroupMessageHistory = extractToGroupMessageHistory(dto.getGroupChatMessageContent(),
                dto.getMessageBody());
        imGroupMessageHistoryMapper.insert(imGroupMessageHistory);
    }


    private ImGroupMessageHistory extractToGroupMessageHistory(GroupChatMessageContent messageContent,
                                                               ImMessageBody imMessageBody) {

        ImGroupMessageHistory imGroupMessageHistory = new ImGroupMessageHistory();
        BeanUtils.copyProperties(messageContent, imGroupMessageHistory);

        imGroupMessageHistory.setGroupId(messageContent.getGroupId());
        imGroupMessageHistory.setMessageKey(imMessageBody.getMessageKey());
        imGroupMessageHistory.setCreateTime(System.currentTimeMillis());
        imGroupMessageHistory.setMessageTime(imMessageBody.getMessageTime());

        return imGroupMessageHistory;
    }
}
