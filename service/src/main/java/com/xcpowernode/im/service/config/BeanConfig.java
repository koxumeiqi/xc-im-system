package com.xcpowernode.im.service.config;

import com.xcpower.im.enums.ImUrlRouteWayEnum;
import com.xcpower.im.enums.RouteHashMethodEnum;
import com.xcpower.im.route.RouteHandle;
import com.xcpower.im.route.strategy.LoopHandle;
import com.xcpower.im.route.strategy.consistenthash.AbstractConsistentHash;
import com.xcpower.im.route.strategy.consistenthash.ConsistentHashHandle;
import com.xcpower.im.route.strategy.consistenthash.TreeMapConsistentHash;
import com.xcpowernode.im.service.utils.SnowflakeIdWorker;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Autowired
    private AppConfig appConfig;

    @Bean
    public ZkClient zkClient() {
        return new ZkClient(appConfig.getZkAddr(), appConfig.getZkConnectTimeOut());
    }

    @Bean
    public RouteHandle randomHandle() {
        RouteHandle routeHandle = null;
        try {
            Integer imRouteWay = appConfig.getImRouteWay();
            ImUrlRouteWayEnum handler = ImUrlRouteWayEnum.getHandler(imRouteWay);
            String routeWay = handler.getClazz();

            routeHandle = (RouteHandle) Class.forName(routeWay).getConstructor().newInstance();

            if (imRouteWay == ImUrlRouteWayEnum.HASH.getCode()) { // 如果是hash负载均衡策略的话，得添加一次性hash算法
                ConsistentHashHandle handle = (ConsistentHashHandle) routeHandle;
                Integer consistentHashWay = appConfig.getConsistentHashWay();
                RouteHashMethodEnum routeHashMethodEnum = RouteHashMethodEnum.getHandler(consistentHashWay);
                String clazz = routeHashMethodEnum.getClazz();
                // 获取hash一致性算法策略实现
                AbstractConsistentHash hash = (AbstractConsistentHash) Class.forName(clazz).getConstructor().newInstance();
                /*配置到hash负载均衡策略中*/
                handle.setHash(hash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return routeHandle;
    }

    @Bean
    public SnowflakeIdWorker buildSnowflakeSeq() throws Exception {
        return new SnowflakeIdWorker(0);
    }

}
