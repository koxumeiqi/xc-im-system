package com.xcpowernode.im.service.conversation.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * im_conversation_set
 * @author 
 */
@Data
public class ImConversationSetKey implements Serializable {
    private Integer appId;

    private String conversationId;

    private static final long serialVersionUID = 1L;
}