package com.xcpowernode.im.service.friendship.entity;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

/**
 * im_friendship
 * @author 
 */
@Data
public class ImFriendshipKey implements Serializable {

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

    private static final long serialVersionUID = 1L;
}