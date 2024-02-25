package com.xcpowernode.im.service.conversation.dao;

import com.xcpower.im.model.SyncReq;
import com.xcpowernode.im.service.conversation.entity.ImConversationSet;
import com.xcpowernode.im.service.conversation.entity.ImConversationSetKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;


@Mapper
public interface ImConversationSetDao {

    ImConversationSet selectByPrimaryKey(ImConversationSetKey key);

    @Update(" update im_conversation_set set readed_sequence = #{readedSequence},sequence = #{sequence} " +
            " where conversation_id = #{conversationId} and app_id = #{appId} AND readed_sequence < #{readedSequence}")
    void readMark(ImConversationSet imConversationSet);

    int insert(ImConversationSet imConversationSet);

    int updateConversation(ImConversationSet imConversationSet);

    List<ImConversationSet> syncConversationSet(SyncReq req);

    @Select("select max(sequence) from im_conversation_set" +
            " where app_id = #{appId} and from_id = #{operator}")
    Long geConversationSetMaxSeq(@Param("appId") Integer appId,
                                 @Param("operator") String operator);
}