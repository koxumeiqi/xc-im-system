package com.xcpowernode.im.service.group.dao;

import com.xcpowernode.im.service.group.entity.ImGroup;
import com.xcpowernode.im.service.group.entity.ImGroupMember;
import com.xcpowernode.im.service.group.model.req.GroupMemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ImGroupMemberMapper {

    int deleteByPrimaryKey(Long groupMemberId);

    int insert(ImGroupMember record);

    int insertSelective(ImGroupMember record);

    ImGroupMember selectByPrimaryKey(Long groupMemberId);

    int updateByPrimaryKeySelective(ImGroupMember record);

    int updateByPrimaryKey(ImGroupMember record);

    ImGroupMember selectOne(ImGroupMember imGroupMember);

    int selectCount(ImGroupMember imGroupMember);

    List<ImGroupMember> selectAllGroupMember(@Param("groupId") String groupId, @Param("appId") Integer appId);

    List<ImGroupMember> selectJoinedGroup(@Param("memberId") String memberId, @Param("appId") Integer appId);

    List<String> getJoinedGroupId(@Param("appId") Integer appId, @Param("memberId") String memberId);

    int updateOwnerRole(@Param("ordinaryRole") int ordinaryRole, @Param("updateRole") int updateRole, @Param("appId") int appId, @Param("groupId") String groupId);

    int updateMemberRole(@Param("updateRole") int updateRole, @Param("appId") int appId, @Param("memberId") String memberId, @Param("groupId") String groupId);

    int updateGroupMemberInfo(ImGroupMember update);

    List<String> getGroupMemberId(@Param("appId") Integer appId, @Param("groupId") String groupId);

    List<GroupMemberDto> getGroupManager(@Param("groupId") String groupId, @Param("appId") Integer appId);

    @Select("select group_id from im_group_member " +
            "where app_id = #{appId} AND member_id = #{memberId} " +
            "and role != #{role}")
    public List<String> syncJoinedGroupId(Integer appId, String memberId, int role);
}