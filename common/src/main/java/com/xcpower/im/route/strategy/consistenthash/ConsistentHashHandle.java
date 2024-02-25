package com.xcpower.im.route.strategy.consistenthash;

import com.xcpower.im.route.RouteHandle;

import java.util.List;

public class ConsistentHashHandle implements RouteHandle {

    // TreeMap 实现的一致性hash算法
    private AbstractConsistentHash hash;

    public void setHash(AbstractConsistentHash hash) {
        this.hash = hash;
    }

    @Override
    public String routeServer(List<String> values, String key) {
        return hash.process(values,key);
    }
}
