package com.xcpowernode.im.service.user.model.req;

import com.xcpower.im.model.RequestBase;
import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import lombok.Data;

import java.util.List;

@Data
public class ImportUserReq extends RequestBase {
    private List<ImUserDataEntity> userData;
}
