package com.xcpowernode.im.service.user.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 用户导入后的响应
 */
@Data
@Builder
public class ImportUserResp {

    private List<String> sucessId;

    private List<String> errorId;

}
