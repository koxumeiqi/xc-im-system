package org.example.message.store.entity;

import java.io.Serializable;
import lombok.Data;

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