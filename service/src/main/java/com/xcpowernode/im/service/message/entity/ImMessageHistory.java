package com.xcpowernode.im.service.message.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * im_message_history
 * @author 
 */
@Data
public class ImMessageHistory extends ImMessageHistoryKey implements Serializable {
    /**
     * from_id
     */
    private String fromId;

    /**
     * to_id

     */
    private String toId;

    private Long createTime;

    private Long sequence;

    private Integer messageRandom;

    /**
     * 来源
     */
    private Long messageTime;

    private static final long serialVersionUID = 1L;
}