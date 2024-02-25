package com.xcpowernode.im.service.friendship.dao;

import com.xcpowernode.im.service.friendship.entity.ImFriendshipRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImFriendshipRequestMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ImFriendshipRequest record);

    int insertSelective(ImFriendshipRequest record);

    ImFriendshipRequest selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ImFriendshipRequest record);

    int updateByPrimaryKey(ImFriendshipRequest record);

    ImFriendshipRequest queryOneReqest(@Param("fromId") String fromId,@Param("toId") String toId,@Param("appId") Integer appId);

    List<ImFriendshipRequest> queryFriendRequestList(@Param("toId") String toId,@Param("appId") Integer appId);

    int updateReadStatus(ImFriendshipRequest update);
}