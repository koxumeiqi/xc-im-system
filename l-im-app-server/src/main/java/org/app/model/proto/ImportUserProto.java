package org.app.model.proto;

import lombok.Data;

import java.util.List;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
public class ImportUserProto {

    private List<UserData> userData;

    @Data
    public static class UserData{
        // 用户id
        private String userId;

        // 用户名称
        private String nickName;

        private String password;

        // 头像
        private String photo;

        // 性别
        private Integer userSex;

        // 个性签名
        private String selfSignature;

        // 加好友验证类型（Friend_AllowType） 1需要验证
        private Integer friendAllowType;

        // 管理员禁止用户添加加好友：0 未禁用 1 已禁用
        private Integer disableAddFriend;

        // 禁用标识(0 未禁用 1 已禁用)
        private Integer forbiddenFlag;

        // 禁言标识
        private Integer silentFlag;
        /**
         * 用户类型 1普通用户 2客服 3机器人
         */
        private Integer userType;
    }
}
