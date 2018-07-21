package com.al.crm.redis.cluster;


import com.al.crm.redis.route.RouteContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DefaultIRedisFixProxyTest {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:spring/spring.xml");
        AbstractIRedisFixProxy proxy = (AbstractIRedisFixProxy) context.getBean("provRedisCluster");
        RouteContext.setRouteId("8320100");
        proxy.set("kekeke","kekeke");
        System.out.println(proxy.get("kekeke"));
        RouteContext.setRouteId("8440100");
        proxy.set("kekeke","kekeke01");
        System.out.println(proxy.get("kekeke"));
    }


}
