package com.xcpowernode.im.service.group.model.resp;

import lombok.Data;

@Data
public class GetRoleInGroupResp {

    private Long groupMemberId;

    private String memberId;

    private Integer role;

    private Long speakDate;

}
