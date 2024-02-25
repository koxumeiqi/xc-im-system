package com.xcpowernode.im.service.group.dao;

import com.xcpower.im.model.SyncReq;
import com.xcpowernode.im.service.group.entity.ImGroup;
import com.xcpowernode.im.service.group.entity.ImGroupKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ImGroupMapper {
    int deleteByPrimaryKey(ImGroupKey key);

    int insert(ImGroup record);

    int insertSelective(ImGroup record);

    ImGroup selectByPrimaryKey(@Param("group") ImGroupKey key, @Param("status") Integer status);

    int updateByPrimaryKeySelective(ImGroup record);

    int updateByPrimaryKey(ImGroup record);

    List<ImGroup> selectList(@Param("appId") Integer appId, @Param("groupIds") Collection<String> data, @Param("groupTypes") List<Integer> groupType);

    List<ImGroup> syncJoinedGroupList(@Param("req") SyncReq req,
                                      @Param("groupIds") Collection<String> data);

    Long getGroupMaxSeq(@Param("groupIds") Collection<String> data,
                        @Param("appId") Integer appId);
}