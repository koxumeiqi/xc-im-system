package com.xcpowernode.im.service.group.dao;

import com.xcpowernode.im.service.group.entity.ImGroupMessageHistory;
import com.xcpowernode.im.service.group.entity.ImGroupMessageHistoryKey;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface ImGroupMessageHistoryMapper {
    int deleteByPrimaryKey(ImGroupMessageHistoryKey key);

    int insert(ImGroupMessageHistory record);

    int insertSelective(ImGroupMessageHistory record);

    ImGroupMessageHistory selectByPrimaryKey(ImGroupMessageHistoryKey key);

    int updateByPrimaryKeySelective(ImGroupMessageHistory record);

    int updateByPrimaryKey(ImGroupMessageHistory record);
}