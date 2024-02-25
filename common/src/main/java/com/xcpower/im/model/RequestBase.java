package com.xcpower.im.model;


import lombok.Data;

@Data
public class RequestBase {

    private Integer appId;

    private String operator; // 操作者

    private Integer clientType;

    private String imei;

}
