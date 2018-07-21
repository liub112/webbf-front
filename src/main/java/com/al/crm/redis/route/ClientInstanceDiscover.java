package com.al.crm.redis.route;

/**
 * 
 * @param <T>
 */
public interface ClientInstanceDiscover<T> {
	
	public T determineCurrentClientBean();

}
