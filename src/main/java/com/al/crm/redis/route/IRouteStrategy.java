package com.al.crm.redis.route;

/**
 * 路由策略
 */
public interface IRouteStrategy {
    /**
     * 确定当前使用连接客户端实例名称
     * @return redis源名称
     */
    String determineCurrentRouteName();
}
