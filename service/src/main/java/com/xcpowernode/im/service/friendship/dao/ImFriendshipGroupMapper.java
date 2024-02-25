package com.xcpowernode.im.service.friendship.dao;

import com.xcpowernode.im.service.friendship.entity.ImFriendshipGroup;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImFriendshipGroupMapper {
    int deleteByPrimaryKey(Integer groupId);

    int insert(ImFriendshipGroup record);

    int insertSelective(ImFriendshipGroup record);

    ImFriendshipGroup selectByPrimaryKey(Integer groupId);

    int updateByPrimaryKeySelective(ImFriendshipGroup record);

    int updateByPrimaryKey(ImFriendshipGroup record);

    ImFriendshipGroup selectOne(ImFriendshipGroup imFriendshipGroup);
}