package com.xcpower.im.route.strategy;

import com.xcpower.im.enums.UserErrorCode;
import com.xcpower.im.exception.ApplicationException;
import com.xcpower.im.route.RouteHandle;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轮询策略
 */
public class LoopHandle implements RouteHandle {

    private AtomicLong index = new AtomicLong();

    @Override
    public String routeServer(List<String> values, String key) {

        int size = values.size();
        if (size == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }

        Long l = index.incrementAndGet() % size;
        if (l < 0) {
            index.set(0L);
            l = 0L;
        }

        return values.get(l.intValue());
    }
}
