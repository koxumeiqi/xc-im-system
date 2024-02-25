package com.xcpower.im.utils;


import com.xcpower.im.BaseErrorCode;
import com.xcpower.im.exception.ApplicationException;
import com.xcpower.im.route.RouteInfo;

public class RouteInfoParseUtil {

    public static RouteInfo parse(String info){
        try {
            String[] serverInfo = info.split(":");
            RouteInfo routeInfo =  new RouteInfo(serverInfo[0], Integer.parseInt(serverInfo[1])) ;
            return routeInfo ;
        }catch (Exception e){
            throw new ApplicationException(BaseErrorCode.PARAMETER_ERROR) ;
        }
    }
}
