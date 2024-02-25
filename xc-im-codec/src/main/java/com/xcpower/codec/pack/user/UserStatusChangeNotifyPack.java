package com.xcpower.codec.pack.user;


import com.xcpower.im.model.UserSession;
import lombok.Data;

import java.util.List;


/**
 * 用户在线状态改变包
 */
@Data
public class UserStatusChangeNotifyPack {

    private Integer appId;

    private String userId;

    private Integer status;

    private List<UserSession> client;

}
