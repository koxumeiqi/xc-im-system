package com.xcpowernode.im.service.group.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * im_group_member
 * @author 
 */
@Data
public class ImGroupMember implements Serializable {
    private Long groupMemberId;

    /**
     * group_id
     */
    private String groupId;

    private Integer appId;

    /**
     * 成员id

     */
    private String memberId;

    /**
     * 群成员类型，0 普通成员, 1 管理员, 2 群主， 3 禁言，4 已经移除的成员
     */
    private Integer role;

    private Long speakDate;

    /**
     * 是否全员禁言，0 不禁言；1 全员禁言
     */
    private Integer mute;

    /**
     * 群昵称
     */
    private String alias;

    /**
     * 加入时间
     */
    private Long joinTime;

    /**
     * 离开时间
     */
    private Long leaveTime;

    /**
     * 加入类型
     */
    private String joinType;

    private String extra;

    private static final long serialVersionUID = 1L;
}