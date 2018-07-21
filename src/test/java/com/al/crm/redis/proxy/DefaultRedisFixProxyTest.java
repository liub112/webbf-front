package com.al.crm.redis.proxy;

import com.al.crm.nosql.cache.IRedisFix;
import com.al.crm.redis.RouteContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

public class DefaultRedisFixProxyTest {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:spring/spring.xml");
        IRedisFix provCluster = (IRedisFix) context.getBean("provRedisCluster");
//        RouteContext.setRouteId("8320100");
//        System.out.println(provCluster.set("aaaaa","bbbb1"));
//        System.out.println(provCluster.get("aaaaa"));
//
//        RouteContext.setRouteId("8440100");
//        System.out.println(provCluster.set("aaaaa","bbbb2"));
//        System.out.println(provCluster.get("aaaaa"));

        RouteContext.setRouteId("8450100");
        System.out.println(provCluster.set("aaaaa","bbbb2"));
        System.out.println(provCluster.get("aaaaa"));
    }
}