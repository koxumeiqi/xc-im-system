package org.example.message.store.model;

import com.xcpower.im.model.message.GroupChatMessageContent;
import com.xcpower.im.model.message.ImMessageBodyDto;
import lombok.Data;
import org.example.message.store.entity.ImMessageBody;

@Data
public class DoStoreGroupMessageDto {

    private GroupChatMessageContent groupChatMessageContent;

    private ImMessageBody messageBody;

}
