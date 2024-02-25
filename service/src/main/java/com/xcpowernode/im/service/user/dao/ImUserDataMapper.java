package com.xcpowernode.im.service.user.dao;

import com.xcpower.im.enums.DelFlagEnum;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import com.xcpowernode.im.service.user.model.req.DeleteUserReq;
import com.xcpowernode.im.service.user.model.req.GetUserInfoReq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImUserDataMapper {

    int deleteByPrimaryKey(ImUserDataEntity key);

    int insert(ImUserDataEntity record);

    int insertSelective(ImUserDataEntity record);

    int updateByPrimaryKeySelective(ImUserDataEntity record);


    int updateByPrimaryKey(ImUserDataEntity record);

    List<ImUserDataEntity> queryUserInfo(@Param("req") GetUserInfoReq req, @Param("delFlag") int delFlag);

    ImUserDataEntity querySingleUserInfo(@Param("userId") String userId, @Param("appId") Integer appId, @Param("delFlag") Integer delFlag);

    int deleteByUserId(@Param("userId") String userId, @Param("appId") Integer appId, @Param("delFlag") int delFlag);
}
