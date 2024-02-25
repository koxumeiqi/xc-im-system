package com.xcpowernode.im.service.user.model.req;

import lombok.Data;

import javax.validation.constraints.NotNull;


@Data
public class LoginReq {

    @NotNull(message = "用户id不能位空")
    private String userId;

    @NotNull(message = "appId不能为空")
    private Integer appId;

    private Integer clientType;

}
