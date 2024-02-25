package com.xcpowernode.im.service.group.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * im_group
 * @author 
 */
@Data
public class ImGroupKey implements Serializable {
    /**
     * app_id
     */
    private Integer appId;

    /**
     * group_id
     */
    private String groupId;

    private static final long serialVersionUID = 1L;
}