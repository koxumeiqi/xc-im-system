package org.app.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.common.ResponseVO;
import org.app.dao.User;
import org.app.dao.mapper.UserMapper;
import org.app.enums.ErrorCode;
import org.app.exception.ApplicationException;
import org.app.model.dto.ImUserDataDto;
import org.app.model.req.RegisterReq;
import org.app.model.req.SearchUserReq;
import org.app.model.resp.ImportUserResp;
import org.app.service.ImService;
import org.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author: Chackylee
 * @description:
 **/
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    ImService imService;

    /**
     * @return com.lld.app.common.ResponseVO
     * @description 根据用户名和密码获取用户
     * @author chackylee
     */
    @Override
    public ResponseVO getUserByUserNameAndPassword(String userName, String password) {

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_name", userName);
        wrapper.eq("password", password);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return ResponseVO.errorResponse(ErrorCode.USER_NOT_EXIST);
        }

        return ResponseVO.successResponse(user);
    }

    /**
     * @return com.lld.app.common.ResponseVO
     * @description 根据手机号获取用户
     * @author chackylee
     */
    @Override
    public ResponseVO getUserByMobile(String mobile) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("mobile", mobile);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return ResponseVO.errorResponse(ErrorCode.USER_NOT_EXIST);
        }
        return ResponseVO.successResponse(user);
    }

    @Override
    public ResponseVO<User> getUserByUserName(String userName) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_name", userName);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return ResponseVO.errorResponse(ErrorCode.USER_NOT_EXIST);
        }
        return ResponseVO.successResponse(user);
    }

    /**
     * @return com.lld.app.common.ResponseVO
     * @description 根据用户id获取用户
     * @author chackylee
     */
    @Override
    public ResponseVO getUserById(Integer userId) {

        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseVO.errorResponse(ErrorCode.USER_NOT_EXIST);
        }
        return ResponseVO.successResponse(user);

    }

    @Override
    @Transactional
    public ResponseVO<User> registerUser(RegisterReq req) {

        User user = new User();
        user.setCreateTime(System.currentTimeMillis());
//        user.setMobile(req.getUserName());
        user.setPassword(req.getPassword());
        user.setUserName(req.getUserName());
        user.setUserId(req.getUserName());
        userMapper.insert(user);

        ArrayList<User> users = new ArrayList<>();
        users.add(user);
        ResponseVO responseVO = imService.importUser(users);
        if(responseVO.isOk()){
            Object data = responseVO.getData();
            ObjectMapper objectMapper = new ObjectMapper();
            ImportUserResp importUserResp = objectMapper.convertValue(data, ImportUserResp.class);

            Set<String> successId = importUserResp.getSuccessId();
            if(successId.contains(user.getUserId().toString())){
                return ResponseVO.successResponse(user);
            }else {
                throw new ApplicationException(ErrorCode.REGISTER_ERROR);
            }
        }else{
            throw new ApplicationException(responseVO.getCode(),responseVO.getMsg());
        }
    }

    @Override
    public ResponseVO searchUser(SearchUserReq req) {

        List<String> userIds = userMapper.searchUser(req);

        //手机号搜索
//        if(req.getSearchType() == 1){
//            userIds = userMapper.searchUser(req);
//        }else if(){
//
//        }
        ResponseVO<ImUserDataDto> userInfo = imService.getUserInfo(userIds);

        return userInfo;
    }
}
