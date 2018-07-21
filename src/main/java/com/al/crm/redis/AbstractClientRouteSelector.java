package com.al.crm.redis;

import com.al.crm.nosql.cache.IRedisFix;
import com.al.crm.redis.strategy.IRouteStrategy;
import com.al.crm.redis.utils.Assert;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractClientRouteSelector<T> implements ClientRouteSelector<T> , InitializingBean {

    /**
     * 缓存Client集合
     */
    private Map<String,T> resloveClientInstanceBeans;

    private  Class<IRouteStrategy> strategy;
    /**
     * 路由策略
     */
    private IRouteStrategy routeStrategy;


    public void setResloveClientInstanceBeans(Map<String, T> resloveClientInstanceBeans) {
        this.resloveClientInstanceBeans = resloveClientInstanceBeans;
    }

    public void setStrategy(Class<IRouteStrategy> strategy) {
        if(!IRouteStrategy.class.isAssignableFrom(strategy))throw new IllegalArgumentException("指定的路由策略实现类："+ strategy.getName()+" 不正确，原因：必须实现接口：com.al.crm.redis.route.IRouteStrategy");
        try{
           this.routeStrategy = strategy.newInstance();
        }catch(IllegalAccessException e){
            throw new IllegalArgumentException("加载路由策略实现类："+ strategy.getName()+" 异常，原因："+e.getMessage());
        }catch(InstantiationException e){
            throw new IllegalArgumentException("实例化路由策略实现类："+ strategy.getName()+" 异常，原因："+e.getMessage());
        }
    }

    @Override
    public T determineCurrentClientBean() {
        String name;
        try{
            name= routeStrategy.determineCurrentRouteName();
            Assert.notNull(name,"请指定当前路由的redis数据源名称");
            if(!this.resloveClientInstanceBeans.containsKey(name))throw new IllegalArgumentException("redis数据源："+name+" 不存在");
            RouteContext.setName(name);
            return this.resloveClientInstanceBeans.get(name);
        }finally{
            name=null;
        }
    }

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if(this.routeStrategy == null){
            throw  new IllegalArgumentException("Property 'routeStrategy' is required");
        }
        if(this.resloveClientInstanceBeans == null){
            throw  new IllegalArgumentException("Property 'resloveClientInstanceBeans' is required");
        }
    }

}
