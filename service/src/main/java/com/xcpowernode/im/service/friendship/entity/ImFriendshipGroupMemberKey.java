package com.xcpowernode.im.service.friendship.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * im_friendship_group_member
 * @author 
 */
@Data
public class ImFriendshipGroupMemberKey implements Serializable {
    private Long groupId;

    private String toId;

    private static final long serialVersionUID = 1L;
}