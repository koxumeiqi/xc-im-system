package org.app.enums;

public enum LoginTypeEnum {

    /**
     * 1 username；2 验证码 3手机号+验证码
     */
    USERNAME_PASSWORD(1),

    SMS_CODE(2),

    SMS_PASSWORD(3)
    ;

    private int code;

    LoginTypeEnum(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
