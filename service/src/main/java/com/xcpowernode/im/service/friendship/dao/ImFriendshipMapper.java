package com.xcpowernode.im.service.friendship.dao;

import com.xcpower.im.ResponseVO;
import com.xcpower.im.model.SyncReq;
import com.xcpowernode.im.service.friendship.entity.ImFriendship;
import com.xcpowernode.im.service.friendship.entity.ImFriendshipKey;
import com.xcpowernode.im.service.friendship.model.req.AddFriendShipBlackReq;
import com.xcpowernode.im.service.friendship.model.req.CheckFriendShipReq;
import com.xcpowernode.im.service.friendship.model.req.DeleteBlackReq;
import com.xcpowernode.im.service.friendship.model.resp.CheckFriendShipResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImFriendshipMapper {
    int deleteByPrimaryKey(ImFriendshipKey key);

    int insert(ImFriendship record);

    int insertSelective(ImFriendship record);

    ImFriendship selectByPrimaryKey(ImFriendshipKey key);

    int updateByPrimaryKeySelective(ImFriendship record);

    int updateByPrimaryKey(ImFriendship record);

    List<ImFriendship> queryAllNormalFriend(ImFriendship imFriendship);

    int deleteAll(@Param("imFriendship") ImFriendship imFriendship, @Param("updateStatus") int updateStatus);

    ImFriendship queryOneNormalFriend(ImFriendship imFriendship);

    List<CheckFriendShipResp> checkFriendShip(@Param("req") CheckFriendShipReq req);

    List<CheckFriendShipResp> checkFriendShipBoth(CheckFriendShipReq req);

    List<CheckFriendShipResp> checkFriendShipBlack(CheckFriendShipReq req);

    List<CheckFriendShipResp> checkFriendShipBlackBoth(CheckFriendShipReq req);

    List<ImFriendship> syncFriendshipList(SyncReq req);

    @Select("select max(friend_sequence) from im_friendship" +
            " where app_id = #{appId} and from_id = #{userId}")
    Long getFriendShipMaxSeq(@Param("appId") Integer appId, @Param("userId") String userId);

    @Select("select to_id from im_friendship " +
            "where app_id = #{appId} and from_id = #{userId}")
    List<String> getAllFriendIds(@Param("userId") String userId, @Param("appId") Integer appId);
}