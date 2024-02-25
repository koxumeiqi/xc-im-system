package org.app.model.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
public class RegisterReq {

    private String userName;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotNull(message = "请选择注册方式")
    //注册方式 1手机号注册 2用户名
    private Integer registerType;

    private String proto;

}
