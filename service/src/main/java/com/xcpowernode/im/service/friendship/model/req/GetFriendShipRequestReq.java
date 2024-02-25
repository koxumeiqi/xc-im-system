package com.xcpowernode.im.service.friendship.model.req;

import com.xcpower.im.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class GetFriendShipRequestReq extends RequestBase {

    @NotBlank(message = "用户id不能为空")
    private String fromId;

}
