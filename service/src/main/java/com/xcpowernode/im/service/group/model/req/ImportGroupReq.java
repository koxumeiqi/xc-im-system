package com.xcpowernode.im.service.group.model.req;

import com.xcpower.im.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ImportGroupReq extends RequestBase {

    /**
     * group_id
     */
    private String groupId;

    /**
     * 群主

     */
    private String ownerId;

    /**
     * 群类型 1私有群（类似微信） 2公开群(类似qq）
     */
    private Integer groupType;

    @NotBlank(message = "群名称不能为空")
    private String groupName;

    /**
     * 是否全员禁言，0 不禁言；1 全员禁言
     */
    private Integer mute;

    /**
     * //    申请加群选项包括如下几种：
     //    0 表示禁止任何人申请加入
     //    1 表示需要群主或管理员审批
     //    2 表示允许无需审批自由加入群组
     */
    private Integer applyJoinType;

    private String photo;

    private Integer maxMemberCount;

    /**
     * 群简介
     */
    private String introduction;

    /**
     * 群公告
     */
    private String notification;

    /**
     * 群状态 0正常 1解散
     */
    private Integer status;

    /***
     * 最大成员数量
     */
    private Integer MaxMemberCount;

    private Long sequence;

    private Long createTime;

    /**
     * 来源
     */
    private String extra;

    private Long updateTime;

}
