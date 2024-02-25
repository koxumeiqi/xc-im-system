package org.example.message.store.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.message.store.entity.ImGroupMessageHistory;
import org.example.message.store.entity.ImGroupMessageHistoryKey;


@Mapper
public interface ImGroupMessageHistoryMapper {
    int deleteByPrimaryKey(ImGroupMessageHistoryKey key);

    int insert(ImGroupMessageHistory record);

    int insertSelective(ImGroupMessageHistory record);

    ImGroupMessageHistory selectByPrimaryKey(ImGroupMessageHistoryKey key);

    int updateByPrimaryKeySelective(ImGroupMessageHistory record);

    int updateByPrimaryKey(ImGroupMessageHistory record);
}