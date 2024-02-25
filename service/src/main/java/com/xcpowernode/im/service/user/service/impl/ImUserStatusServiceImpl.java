package com.xcpowernode.im.service.user.service.impl;


import com.xcpower.codec.pack.user.UserStatusChangeNotifyPack;
import com.xcpower.im.enums.command.UserEventCommand;
import com.xcpower.im.model.UserSession;
import com.xcpowernode.im.service.friendship.service.ImFriendShipService;
import com.xcpowernode.im.service.message.service.MessageSyncService;
import com.xcpowernode.im.service.user.model.UserStatusChangeNotifyContent;
import com.xcpowernode.im.service.user.service.ImUserStatusService;
import com.xcpowernode.im.service.utils.MessageProducer;
import com.xcpowernode.im.service.utils.UserSessionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
public class ImUserStatusServiceImpl implements ImUserStatusService {


    @Autowired
    private UserSessionUtils userSessionUtils;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private ImFriendShipService imFriendShipService;

    @Override
    public void processUserOnlineStatusNotify(UserStatusChangeNotifyContent content) {

        // TODO 同步自身其他端
        List<UserSession> userSessions = userSessionUtils.getUserSession(content.getAppId(),
                content.getUserId());
        UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
        BeanUtils.copyProperties(content, userStatusChangeNotifyPack);
        userStatusChangeNotifyPack.setClient(userSessions);

        syncSender(userStatusChangeNotifyPack,
                content.getUserId(),
                content);

        // TODO 同步好友和订阅端
        dispatcher(userStatusChangeNotifyPack, content.getUserId(),
                content.getAppId());

    }

    private void dispatcher(UserStatusChangeNotifyPack userStatusChangeNotifyPack,
                            String userId,
                            Integer appId) {

        // 查找对应的好友Ids
        List<String> friendIds = imFriendShipService.getAllFriendId(userId, appId);
        for (String fd : friendIds) {
            messageProducer.sendToUser(
                    fd,
                    UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY
                    , userStatusChangeNotifyPack,
                    appId);
        }


    }

    private void syncSender(UserStatusChangeNotifyPack userStatusChangeNotifyPack, String userId, UserStatusChangeNotifyContent content) {
        messageProducer.sendToUserExceptClient(userId,
                UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC,
                userStatusChangeNotifyPack,
                content);
    }
}
