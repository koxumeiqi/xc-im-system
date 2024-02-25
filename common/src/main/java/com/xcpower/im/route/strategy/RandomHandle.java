package com.xcpower.im.route.strategy;

import com.xcpower.im.enums.UserErrorCode;
import com.xcpower.im.exception.ApplicationException;
import com.xcpower.im.route.RouteHandle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机的负载均衡
 */
public class RandomHandle implements RouteHandle {

    @Override
    public String routeServer(List<String> values, String key) {

        int size = values.size();

        if (size == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        // 去获取一个小于size的索引值
        // 你可以简单理解：随机整型数&size
        int index = ThreadLocalRandom.current().nextInt(size);
        return values.get(index);
    }
}
