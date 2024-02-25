package com.xcpowernode.im.service.group.model.req;

import com.xcpower.im.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GetGroupReq extends RequestBase {

    @NotBlank(message = "群ID不能为空")
    private String groupId;

}
