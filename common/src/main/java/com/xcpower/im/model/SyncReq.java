package com.xcpower.im.model;

import lombok.Data;

@Data
public class SyncReq extends RequestBase {

    //客户端最大seq
    private Long lastSequence;
    //一次拉取多少
    private Integer maxLimit;

}
