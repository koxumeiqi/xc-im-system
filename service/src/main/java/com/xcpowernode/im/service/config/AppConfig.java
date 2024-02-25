package com.xcpowernode.im.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "appconfig")
public class AppConfig {

    private String privateKey;

    /**
     * zk连接地址
     */
    private String zkAddr;

    /**
     * zk连接超时时间
     */
    private Integer zkConnectTimeOut;

    /**
     * 路由策略 1随机 2轮询 3hash
     */
    private Integer imRouteWay;

    private boolean sendMessageCheckBlack; // 发送消息是否校验黑名单

    private boolean sendMessageCheckFriend; // 发送消息是否校验关系链

    /**
     * 如果选用一致性hash的话具体hash算法 1 TreeMap 2 自定义Map,在RouteHashMethodEnum中配置
     */
    private Integer consistentHashWay;

    private String callbackUrl;

    private boolean modifyUserAfterCallback; //用户资料变更之后回调开关

    private boolean addFriendAfterCallback; //添加好友之后回调开关

    private boolean addFriendBeforeCallback; //添加好友之前回调开关

    private boolean modifyFriendAfterCallback; //修改好友之后回调开关

    private boolean deleteFriendAfterCallback; //删除好友之后回调开关

    private boolean addFriendShipBlackAfterCallback; //添加黑名单之后回调开关

    private boolean deleteFriendShipBlackAfterCallback; //删除黑名单之后回调开关

    private boolean createGroupAfterCallback; //创建群聊之后回调开关

    private boolean modifyGroupAfterCallback; //修改群聊之后回调开关

    private boolean destroyGroupAfterCallback;//解散群聊之后回调开关

    private boolean deleteGroupMemberAfterCallback;//删除群成员之后回调

    private boolean addGroupMemberBeforeCallback;//拉人入群之前回调

    private boolean addGroupMemberAfterCallback;//拉人入群之后回调

    private boolean sendMessageAfterCallback;//发送单聊消息之后

    private boolean sendMessageBeforeCallback;//发送单聊消息之前

    private Integer offlineMessageCount;//离线消息最大条数

    private boolean deleteConversationSyncMode; // 用来判断是否启动同步删除会话模式



}
