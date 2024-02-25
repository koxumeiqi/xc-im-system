package com.xcpowernode.im.service.friendship.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * im_friendship_request
 * @author 
 */
@Data
public class ImFriendshipRequest implements Serializable {
    /**
     * id
     */
    private Integer id;

    /**
     * app_id
     */
    private Integer appId;

    /**
     * from_id
     */
    private String fromId;

    /**
     * to_id
     */
    private String toId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否已读 1已读
     */
    private Integer readStatus;

    /**
     * 好友来源
     */
    private String addSource;

    /**
     * 好友验证信息
     */
    private String addWording;

    /**
     * 审批状态 1同意 2拒绝
     */
    private Integer approveStatus;

    private Long createTime;

    private Long updateTime;

    private Long sequence;

    private static final long serialVersionUID = 1L;
}