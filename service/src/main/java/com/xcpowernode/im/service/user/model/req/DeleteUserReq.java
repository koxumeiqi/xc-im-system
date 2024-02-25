package com.xcpowernode.im.service.user.model.req;


import com.xcpower.im.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;


@Data
public class DeleteUserReq extends RequestBase {

    @NotEmpty(message = "用户id不能为空")
    private List<String> userIds;
}
