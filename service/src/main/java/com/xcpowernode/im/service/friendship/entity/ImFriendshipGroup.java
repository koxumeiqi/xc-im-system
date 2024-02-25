package com.xcpowernode.im.service.friendship.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * im_friendship_group
 * @author 
 */
@Data
public class ImFriendshipGroup implements Serializable {
    private Integer groupId;

    /**
     * app_id
     */
    private Integer appId;

    /**
     * from_id
     */
    private String fromId;

    private String groupName;

    private Long sequence;

    private Long createTime;

    private Long updateTime;

    private Integer delFlag;

    private static final long serialVersionUID = 1L;
}