package org.example.message.store.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * im_message_body
 * @author 
 */
@Data
public class ImMessageBody implements Serializable {
    private Long messageKey;

    private Integer appId;

    private String messageBody;

    private String securityKey;

    private Long messageTime;

    private Long createTime;

    private String extra;

    private Integer delFlag;

    private static final long serialVersionUID = 1L;
}