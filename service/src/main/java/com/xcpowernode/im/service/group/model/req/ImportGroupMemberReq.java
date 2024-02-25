package com.xcpowernode.im.service.group.model.req;

import com.xcpower.im.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;


@Data
public class ImportGroupMemberReq extends RequestBase {

    @NotBlank(message = "群id不能为空")
    private String groupId;

    private List<GroupMemberDto> members;

}
