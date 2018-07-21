package com.al.crm.redis.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 客户端实例选择器
 * @author think
 */
public class DefaultClientInstanceDiscover<T> implements ClientInstanceDiscover<T>,InitializingBean {
    private static Logger LOG = LoggerFactory.getLogger(DefaultClientInstanceDiscover.class);
    /**
     * 客户端集合
     */
    private Map<String,Object> targetClientInstanceBeans;

    /**
     * 解析后端集合
     */
    private Map<String,T> resloveClientInstanceBeans;

    /**
     * 路由策略
     */
    private IRouteStrategy strategy;


    public static final List<String> RESOLVED_CLIENT_NAME=new LinkedList<String>();


    /**
     * Sets the targetClientInstanceBeans
     * <p>You can use getTargetClientInstanceBeans() to get the value of targetClientInstanceBeans</p>
     *
     * @param targetClientInstanceBeans targetClientInstanceBeans
     */
    public void setTargetClientInstanceBeans(Map<String, Object> targetClientInstanceBeans) {
        this.targetClientInstanceBeans = targetClientInstanceBeans;
    }

    /**
     * Sets the strategy
     * <p>You can use getRouteStrategy() to get the value of strategy</p>
     *
     * @param strategy strategy
     */
    public void setStrategy(Class<IRouteStrategy> strategy) {
        if(!IRouteStrategy.class.isAssignableFrom(strategy))throw new IllegalArgumentException("指定的路由策略实现类："+ strategy.getName()+" 不正确，原因：必须实现接口：com.al.crm.redis.route.IRouteStrategy");
        try{
            this.strategy =(IRouteStrategy) strategy.newInstance();
        }catch(IllegalAccessException e){
            throw new IllegalArgumentException("加载路由策略实现类："+ strategy.getName()+" 异常，原因："+e.getMessage());
        }catch(InstantiationException e){
            throw new IllegalArgumentException("实例化路由策略实现类："+ strategy.getName()+" 异常，原因："+e.getMessage());
        }
    }

    /**
     *
     * @return
     */
    protected T determineTargetClientBean(){
        String name;
        try{
            name= strategy.determineCurrentRouteName();
            Assert.notNull(name,"请指定当前路由的redis数据源名称");
            if(!this.RESOLVED_CLIENT_NAME.contains(name))throw new IllegalArgumentException("redis数据源："+name+" 不存在");
            RouteContext.setName(name);
            return this.resloveClientInstanceBeans.get(name);
        }finally{
            name=null;
        }
    }


    @Override
    public T determineCurrentClientBean() {
        return determineTargetClientBean();
    }

    public void afterPropertiesSet() throws Exception {
        if(this.targetClientInstanceBeans == null){
            throw  new IllegalArgumentException("Property 'targetClientInstanceBeans' is required");
        }
        if(this.strategy == null){
            throw  new IllegalArgumentException("Property 'strategy' is required");
        }
        resloveClientInstanceBeans = new HashMap<String,T>(targetClientInstanceBeans.size());
        for (Map.Entry<String,Object> entry: this.targetClientInstanceBeans.entrySet()) {
            String lookupKey = entry.getKey();
            RESOLVED_CLIENT_NAME.add(lookupKey);
            Object o = entry.getValue();

            T source = resolveSpecifiedClientSource(o);
            resloveClientInstanceBeans.put(lookupKey,source);
        }
    }

    /**
     * 校验实例化对象是否符合目标源
     */
    private T resolveSpecifiedClientSource(Object source){
        return (T) source;
    }

    public void resloveParameterizedType(){
        Type genType = this.getClass().getGenericSuperclass();
        Class templatClazz = null;

        if(ParameterizedType.class.isInstance(genType))
        {
            //无法获取到User类，或者可能获取到错误的类型，如果有同名且不带包名的泛型存在
            ParameterizedType parameterizedType = (ParameterizedType) genType;
            templatClazz = (Class) parameterizedType.getActualTypeArguments()[0];
        }
    }

}
