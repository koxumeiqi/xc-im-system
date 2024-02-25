package com.xcpower.im.model.message;

import lombok.Data;

@Data
public class OfflineMessageContent {

    private Integer appId;

    /** messageBodyId*/
    private Long messageKey;

    /** messageBody*/
    private String messageBody;

    private Long messageTime;

    private String extra;

    private Integer delFlag;

    private String fromId;

    private String toId;

    /** 序列号*/
    private Long messageSequence;

    private String messageRandom;

    private Integer conversationType;

    private String conversationId;

}