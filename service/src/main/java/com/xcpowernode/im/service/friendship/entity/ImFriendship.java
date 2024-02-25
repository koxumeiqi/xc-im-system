package com.xcpowernode.im.service.friendship.entity;

import java.io.Serializable;

import lombok.*;

import javax.validation.constraints.NotBlank;

/**
 * im_friendship
 * @author 
 */
@Data
public class ImFriendship extends ImFriendshipKey implements Serializable {
    /**
     * 备注
     */
    private String remark;

    /**
     * 状态 1正常 2删除
     */
    private Integer status;

    /**
     * 1正常 2拉黑
     */
    private Integer black;

    private Long createTime;

    /**
     * 好友关系序列号
     */
    private Long friendSequence;

    /**
     * 黑名单序列号
     */
    private Long blackSequence;

    /**
     * 来源
     */
    private String addSource;

    /**
     * 来源
     */
    private String extra;

    private static final long serialVersionUID = 1L;
}