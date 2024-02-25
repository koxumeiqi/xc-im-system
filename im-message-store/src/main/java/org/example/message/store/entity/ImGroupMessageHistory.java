package org.example.message.store.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * im_group_message_history
 * @author 
 */
@Data
public class ImGroupMessageHistory extends ImGroupMessageHistoryKey implements Serializable {
    /**
     * from_id
     */
    private String fromId;

    private Long createTime;

    private Long sequence;

    private Integer messageRandom;

    /**
     * 来源
     */
    private Long messageTime;

    private static final long serialVersionUID = 1L;
}