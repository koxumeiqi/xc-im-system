package com.xcpowernode.im.service.friendship.model.req;

import com.xcpower.im.enums.FriendShipStatusEnum;
import com.xcpower.im.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class AddFriendReq extends RequestBase {

    @NotBlank(message = "fromId不能为空")
    private String fromId;

    private FriendDto toItem;

}
