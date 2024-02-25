package com.xcpower.im.model.message;

import com.xcpower.im.model.ClientInfo;
import lombok.Data;

@Data
public class MessageReadedContent extends ClientInfo {

    private String fromId;

    private String toId;

    private long messageSequence;

    private String groupId;

    private Integer conversationType; // 用于判断是群消息已读还是 P2P 已读,0是后者，1是前者

}
