package com.xcpower.im.model.message;


import lombok.Data;

@Data
public class MessageRecieveServerAckPack {

    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageSequence;

    private boolean serverSend;

}
