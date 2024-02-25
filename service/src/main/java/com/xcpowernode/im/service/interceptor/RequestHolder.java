package com.xcpowernode.im.service.interceptor;

/**
 * @author: Chackylee
 * @description:
 **/
public class RequestHolder {

    private final static ThreadLocal<Boolean> requestHolder = new ThreadLocal<>();

    public static void set(Boolean isadmin) {
        requestHolder.set(isadmin);
    }

    public static Boolean get() {
        return requestHolder.get();
    }

    public static void remove() {
        requestHolder.remove();
    }
}
