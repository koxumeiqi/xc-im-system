package com.xcpowernode.im.service.group.model.resp;

import com.xcpowernode.im.service.group.entity.ImGroup;
import com.xcpowernode.im.service.group.entity.ImGroupMember;
import lombok.Data;

import java.util.List;

@Data
public class GetJoinedGroupResp {

    private Integer totalCount;

    private List<ImGroup> groupList;

}
