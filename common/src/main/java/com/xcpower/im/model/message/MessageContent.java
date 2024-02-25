package com.xcpower.im.model.message;


import com.xcpower.im.model.ClientInfo;
import com.xcpower.im.model.RequestBase;
import lombok.Data;

@Data
public class MessageContent extends ClientInfo {

    private String messageId;

    private String fromId;

    private String toId;

    private String messageBody;

    private Long messageTime;

    private String extra;

    private Long messageKey;

    private long messageSequence;
}
