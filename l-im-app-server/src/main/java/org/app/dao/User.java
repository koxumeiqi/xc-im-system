package org.app.dao;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
@TableName("app_user")
public class User {

    @TableId
    private String userId;
    //用户名
    private String userName;
    //密码
    private String password;
    //手机号
    private String mobile;
    //创建时间
    private Long createTime;
    //更新时间
    private Long updateTime;

}
