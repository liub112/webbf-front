package com.al.crm.redis.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * 默认路由策略
 */
public class DefaultRouteStrategy  implements IRouteStrategy{
    private static Logger LOG = LoggerFactory.getLogger(DefaultRouteStrategy.class);

    public String determineCurrentRouteName() {
        String routeId = RouteContext.getRouteId();
        Assert.notNull(routeId, "入参未发现路由标识");
        String key;
        if("LTE".equals(com.al.crm.telcom.MDA.TELCOM_CUR_SYS)){
            //获取本地网级分库，如果本地网级没有则
            key = RouteContext.CONST_ROUTE_KEY_AREA+"_"+routeId.substring(0, 5);
            if(!MDA.ROUTE_MAPPING.containsKey(key)){
                key = RouteContext.CONST_ROUTE_KEY_AREA+"_"+routeId.substring(0, 3);
            }
        }else{
            key = RouteContext.CONST_ROUTE_KEY_DISTRIBUTOR+"_"+routeId;
        }

        String value = MDA.ROUTE_MAPPING.get(key);
        if(value == null){
            throw new IllegalArgumentException("未匹配分省redis路由异常");
        }
        LOG.debug("get redis instance sucess:{}",value);
        return value;
    }
}
