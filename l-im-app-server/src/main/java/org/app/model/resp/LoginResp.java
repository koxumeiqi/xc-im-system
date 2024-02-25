package org.app.model.resp;

import lombok.Data;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
public class LoginResp {

    //im的token
    private String imUserSign;

    //自己的token
    private String userSign;

    private String userId;

    private Integer appId;

}
