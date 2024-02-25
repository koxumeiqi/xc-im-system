package com.xcpowernode.im.service.friendship.dao;

import com.xcpowernode.im.service.friendship.entity.ImFriendshipGroupMemberKey;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImFriendshipGroupMemberMapper {
    int deleteByPrimaryKey(ImFriendshipGroupMemberKey key);

    int insert(ImFriendshipGroupMemberKey record);

    int insertSelective(ImFriendshipGroupMemberKey record);

    int deleteByGroupId(Long groupId);
}