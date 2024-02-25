package org.example.message.store.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * im_group_message_history
 * @author 
 */
@Data
public class ImGroupMessageHistoryKey implements Serializable {
    /**
     * app_id
     */
    private Integer appId;

    /**
     * group_id
     */
    private String groupId;

    /**
     * messageBodyId
     */
    private Long messageKey;

    private static final long serialVersionUID = 1L;
}