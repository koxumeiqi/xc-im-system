package org.example.message.store.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.message.store.entity.ImMessageBody;

@Mapper
public interface ImMessageBodyMapper {
    int deleteByPrimaryKey(Long messageKey);

    int insertSelective(ImMessageBody record);

    ImMessageBody selectByPrimaryKey(Long messageKey);

    int updateByPrimaryKeySelective(ImMessageBody record);

    int updateByPrimaryKey(ImMessageBody record);
}