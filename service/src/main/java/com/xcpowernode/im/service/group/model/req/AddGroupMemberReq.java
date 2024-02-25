package com.xcpowernode.im.service.group.model.req;

import com.xcpower.im.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class AddGroupMemberReq extends RequestBase {

    @NotBlank(message = "群id不能为空")
    private String groupId;

    @NotEmpty(message = "群成员不能为空")
    private List<GroupMemberDto> members;

}
