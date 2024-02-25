package com.xcpower.im.model.message;

import com.xcpower.im.model.ClientInfo;
import lombok.Data;


@Data
public class MessageReciveAckContent extends ClientInfo {

    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageSequence;


}
