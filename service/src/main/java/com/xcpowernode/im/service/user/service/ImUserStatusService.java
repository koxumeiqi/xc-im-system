package com.xcpowernode.im.service.user.service;


import com.xcpowernode.im.service.user.model.UserStatusChangeNotifyContent;

public interface ImUserStatusService {

    void processUserOnlineStatusNotify(UserStatusChangeNotifyContent content);

}
