package com.xcpowernode.im.service.user.model.req;

import com.xcpower.im.model.RequestBase;
import lombok.Data;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class GetUserSequenceReq extends RequestBase {

    private String userId;

}
