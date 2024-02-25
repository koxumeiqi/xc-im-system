package com.xcpower.tcp.redis;

import com.xcpower.tcp.reciver.UserLoginMessageListener;

public class MqManager {

    public static void listenUserLoginMessage(Integer loginModel) { // 监听登录消息，实现多端登录
        try {
            new UserLoginMessageListener(loginModel).listenerUserLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
