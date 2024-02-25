package com.xcpowernode.im.service.conversation.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * im_conversation_set
 * @author 
 */
@Data
public class ImConversationSet extends ImConversationSetKey implements Serializable {
    /**
     * 0 单聊 1群聊 2机器人 3公众号
     */
    private Integer conversationType;

    private String fromId;

    private String toId;

    /**
     * 是否免打扰 1免打扰
     */
    private Integer isMute;

    /**
     * 是否置顶 1置顶
     */
    private Integer isTop;

    /**
     * sequence
     */
    private Long sequence;

    private Long readedSequence;

    private static final long serialVersionUID = 1L;
}