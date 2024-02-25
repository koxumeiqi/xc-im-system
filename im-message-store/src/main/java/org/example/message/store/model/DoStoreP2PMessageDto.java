package org.example.message.store.model;

import com.xcpower.im.model.message.ImMessageBodyDto;
import com.xcpower.im.model.message.MessageContent;
import lombok.Data;
import org.example.message.store.entity.ImMessageBody;

@Data
public class DoStoreP2PMessageDto {

    private MessageContent messageContent;

    private ImMessageBody messageBody;

}
