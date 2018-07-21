package com.al.crm.redis;

/**
 * 客户端路由选择器
 * @param <T>
 */
public interface ClientRouteSelector<T> {
	
	public T determineCurrentClientBean();

}
