package com.xcpowernode.im.service.user.model.req;

import com.xcpower.im.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class UserId extends RequestBase {
    @NotEmpty(message = "用户ID不能为空")
    private String userId;
}
