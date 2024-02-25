package com.xcpower.im.enums;

public enum ImUserTypeEnum {

    IM_USER(1),

    APP_ADMIN(100),
    ;

    private int code;

    ImUserTypeEnum(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
