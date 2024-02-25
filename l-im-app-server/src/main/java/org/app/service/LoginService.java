package org.app.service;


import org.app.common.ResponseVO;
import org.app.model.req.LoginReq;
import org.app.model.req.RegisterReq;

/**
 * @author: Chackylee
 * @description:
 **/
public interface LoginService {

    public ResponseVO login(LoginReq req);

    public ResponseVO register(RegisterReq req);
}
