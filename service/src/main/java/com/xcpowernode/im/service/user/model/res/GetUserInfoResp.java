package com.xcpowernode.im.service.user.model.res;


import com.xcpowernode.im.service.user.entity.ImUserDataEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
@Builder
public class GetUserInfoResp {

    private List<ImUserDataEntity> userDataItem;

    private List<String> failUser;


}
