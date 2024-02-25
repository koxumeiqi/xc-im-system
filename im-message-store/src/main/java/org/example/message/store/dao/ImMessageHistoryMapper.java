package org.example.message.store.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.message.store.entity.ImMessageHistory;
import org.example.message.store.entity.ImMessageHistoryKey;

import java.util.List;


@Mapper
public interface ImMessageHistoryMapper {
    int deleteByPrimaryKey(ImMessageHistoryKey key);

    int insert(List<ImMessageHistory> records);

    int insertSelective(ImMessageHistory record);

    ImMessageHistory selectByPrimaryKey(ImMessageHistoryKey key);

    int updateByPrimaryKeySelective(ImMessageHistory record);

    int updateByPrimaryKey(ImMessageHistory record);
}