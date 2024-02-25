package com.xcpower.im.model.message;

import lombok.Data;

@Data
public class DoStoreGroupMessageDto {

    private GroupChatMessageContent groupChatMessageContent;

    private ImMessageBodyDto messageBodyDto;

}
