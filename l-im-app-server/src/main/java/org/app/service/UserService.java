package org.app.service;


import org.app.common.ResponseVO;
import org.app.dao.User;
import org.app.model.req.RegisterReq;
import org.app.model.req.SearchUserReq;

public interface UserService {

    public ResponseVO<User> getUserByUserNameAndPassword(String userName, String password);

    public ResponseVO<User> getUserByMobile(String mobile);

    public ResponseVO<User> getUserByUserName(String userName);

    public ResponseVO<User> getUserById(Integer userId);

    public ResponseVO<User> registerUser(RegisterReq req);

    public ResponseVO searchUser(SearchUserReq req);

}
