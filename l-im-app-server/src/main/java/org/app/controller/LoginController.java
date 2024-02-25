package org.app.controller;

import org.app.common.ResponseVO;
import org.app.model.req.LoginReq;
import org.app.model.req.RegisterReq;
import org.app.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Chackylee
 * @description:
 **/
@RestController
@RequestMapping("v1")
public class LoginController {

    @Autowired
    LoginService loginService;

    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req) {

        return loginService.login(req);
    }

    @RequestMapping("/register")
    public ResponseVO register(@RequestBody @Validated RegisterReq req) {
        return loginService.register(req);
    }

}
