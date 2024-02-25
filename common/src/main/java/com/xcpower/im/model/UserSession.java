package com.xcpower.im.model;

import lombok.Data;

@Data
public class UserSession {

    private String userId;

    /**
     * 应用ID
     */
    private Integer appId;

    /**
     * 端的标识
     */
    private Integer clientType;

    //sdk 版本号
    private Integer version;

    //连接状态 1=在线 2=离线
    private Integer connectState;

    private Integer brokerId;

    private String brokerHost;

    /**
     * 设备的唯一标识
     */
    private String imei;

}
