package com.xcpowernode.im.service.friendship.model.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportFriendShipResp {

    private List<String> successIds;
    private List<String> errorIds;

}
