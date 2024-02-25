package com.xcpowernode.im.service.user.model.req;

import com.xcpower.im.model.RequestBase;
import lombok.Data;

import java.util.List;


@Data
public class GetUserInfoReq extends RequestBase {
    private List<String> userIds;
}
