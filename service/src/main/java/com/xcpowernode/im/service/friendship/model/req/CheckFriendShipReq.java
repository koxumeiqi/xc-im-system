package com.xcpowernode.im.service.friendship.model.req;


import com.xcpower.im.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class CheckFriendShipReq extends RequestBase {

    @NotBlank(message = "fromId不能为空")
    private String fromId;

    @NotEmpty(message = "toIds不能为空")
    private List<String> toIds;

    @NotNull(message = "checkType不能为空")
    private Integer checkType;
}
