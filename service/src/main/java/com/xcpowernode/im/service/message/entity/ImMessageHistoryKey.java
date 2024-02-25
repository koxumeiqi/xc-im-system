package com.xcpowernode.im.service.message.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * im_message_history
 * @author 
 */
@Data
public class ImMessageHistoryKey implements Serializable {
    /**
     * app_id
     */
    private Integer appId;

    /**
     * owner_id

     */
    private String ownerId;

    /**
     * messageBodyId
     */
    private Long messageKey;

    private static final long serialVersionUID = 1L;
}