package org.app.service.impl;

import org.app.common.ResponseVO;
import org.app.config.AppConfig;
import org.app.dao.User;
import org.app.enums.ErrorCode;
import org.app.enums.LoginTypeEnum;
import org.app.enums.RegisterTypeEnum;
import org.app.model.req.LoginReq;
import org.app.model.req.RegisterReq;
import org.app.model.resp.LoginResp;
import org.app.service.LoginService;
import org.app.service.UserService;
import org.app.utils.SigAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Chackylee
 * @description:
 **/
@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    UserService userService;

    @Autowired
    AppConfig appConfig;

    /**
     * @return com.lld.app.common.ResponseVO
     * @description 登录服务，需返回im userSign，和app的userSign
     * @author chackylee
     */
    @Override
    public ResponseVO login(LoginReq req) {

        LoginResp loginResp = new LoginResp();

        if (LoginTypeEnum.USERNAME_PASSWORD.getCode() == req.getLoginType()) {
            ResponseVO<User> userResp = userService.getUserByUserNameAndPassword(req.getUserName(), req.getPassword());
            if (userResp.isOk()) {
                User user = userResp.getData();
                SigAPI SigAPI = new SigAPI(appConfig.getAppId(),
                        appConfig.getPrivateKey());
                String s = SigAPI.genUserSig(user.getUserId(),
                        500000);
                loginResp.setImUserSign(s);
                loginResp.setUserSign("asdasdsd");
                loginResp.setUserId(user.getUserId());
            } else if (userResp.getCode() == ErrorCode.USER_NOT_EXIST.getCode()) {
                return ResponseVO.errorResponse(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
            } else {
                return userResp;
            }

        } else if (LoginTypeEnum.SMS_CODE.getCode() == req.getLoginType()) {
            String key = "lld";
        }

        loginResp.setAppId(appConfig.getAppId());
        return ResponseVO.successResponse(loginResp);
    }

    /**
     * @description 注册我们的服务并向im导入用户
     * @author chackylee
     */
    @Override
    @Transactional
    public ResponseVO register(RegisterReq req) {
        if(RegisterTypeEnum.USERNAME.getCode() == req.getRegisterType()){
            ResponseVO<User> userByUserName = userService.getUserByUserName(req.getUserName());
            if(userByUserName.isOk()){
                return ResponseVO.errorResponse(ErrorCode.REGISTER_ERROR);
            }
            ResponseVO<User> userResponseVO = userService.registerUser(req);
            return userResponseVO;
        }
        return ResponseVO.successResponse();
    }


}
