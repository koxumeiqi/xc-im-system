package org.app.common;

import org.app.exception.ApplicationExceptionEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
public class ResponseVO<T> {

    private int code;

    private String msg;

    private T data;

    public static ResponseVO successResponse(Object data) {
        return new ResponseVO(data);
    }

    public static ResponseVO successResponse() {
        return new ResponseVO(200, "success");
    }

    public static ResponseVO errorResponse() {
        return new ResponseVO(500, "系统内部异常");
    }

    public static ResponseVO errorResponse(int code, String msg) {
        return new ResponseVO(code, msg);
    }

    public static ResponseVO errorResponse(ApplicationExceptionEnum enums) {
        return new ResponseVO(enums.getCode(), enums.getError());
    }

    public boolean isOk() {
        return this.code == 200;
    }


    public ResponseVO() {
    }

    public ResponseVO(T data) {
        this.code = 200;
        this.msg = "success";
        this.data = data;
    }

    public ResponseVO(int code, String msg) {
        this.code = code;
        this.msg = msg;
//		this.data = null;
    }

    public ResponseVO success() {
        this.code = 200;
        this.msg = "success";
        return this;
    }

    public ResponseVO success(T data) {
        this.code = 200;
        this.msg = "success";
        this.data = data;
        return this;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setData(T data) {
        this.data = data;
    }
}
