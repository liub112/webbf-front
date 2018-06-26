package com.al.crm.intf.service;

import com.al.common.exception.BaseException;
import com.al.crm.utils.JsonUtil;
import com.al.crm.utils.ReturnsUtil;
import com.ctg.itrdc.cache.pool.CtgJedisPool;
import com.ctg.itrdc.cache.pool.CtgJedisPoolConfig;
import com.ctg.itrdc.cache.pool.ProxyJedis;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

@Service("ctgCacheService")
public class CtgCacheServiceImpl {
	//全量连接池表(需要线程安全)
	private  List<CtgJedisPool> targetRedisSources = new CopyOnWriteArrayList<CtgJedisPool>();
	//常用连接池表(需要线程安全)
	private List<CtgJedisPool> resolvedRedisSources;
	
	private String servers ="10.128.91.224:8088,10.128.91.225:8088";
	
	private String passWord ="crmtest#asia@123";
	
	private Integer maxActive =300;
	
	private Integer maxIdle =300;
	 
	private Integer database=4970;

	private Integer minIdle=30;
	
	private Integer maxWaitMillis=3000;

	private Long priod =3000L;

	private ReentrantLock lock = new ReentrantLock();

	private Boolean broken =false;
	
	/**
	 * 初始化连接池
	 * {
	"database":4970,
	"maxActive":300,
	"maxIdle":30,
	"maxWaitMillis":3000,
	"minIdle":3,
	"passWord":"crmtest#asia@123",
	 "period":3000,
	"servers":"10.128.91.222:8088,10.128.91.223:8088"
	}
	 */
	public String initRedisClientParam(String jsonString){
		try {
			JSONObject json = JSONObject.fromObject(jsonString);
			String errorMsg ="初始化连接池获取参数时";
			String servers =JsonUtil.getStringFromJSON(json, "servers", errorMsg);
			String passWord =JsonUtil.getStringFromJSON(json, "passWord", errorMsg);
			Integer maxActive = JsonUtil.getIntByKey(json, "maxActive");
			Integer maxIdle = JsonUtil.getIntByKey(json, "maxIdle");
			Integer database = JsonUtil.getIntByKey(json, "database");
			Integer minIdle = JsonUtil.getIntByKey(json, "minIdle");
			Integer maxWaitMillis = JsonUtil.getIntByKey(json, "maxWaitMillis");
			Integer period = JsonUtil.getIntByKey(json,"period");
			this.initParams(servers,passWord,maxActive,maxIdle,database,minIdle,maxWaitMillis,period);
		} catch (Exception e) {
			return ReturnsUtil.returnException("初始化接入机连接池获取参数时", e).toString();
		}
		return queryCtgCacheInitParams();
		
	}
	
	/**
	 * 设置参数
	 * @return 
	 */
	private synchronized void initParams(String servers,String passWord,Integer maxActive,Integer maxIdle,Integer database,Integer minIdle,Integer maxWaitMillis,Integer period){
		destory();
		//1.设置初始化参数
		if(StringUtils.isNotBlank(servers))this.servers=servers;
		if(StringUtils.isNotBlank(passWord))this.passWord=passWord;
		if(maxActive!=null)this.maxActive=maxActive;
		if(maxIdle!=null)this.maxIdle=maxIdle;
		if(database!=null)this.database=database;
		if(minIdle!=null)this.minIdle=minIdle;
		if(maxWaitMillis!=null)this.maxWaitMillis=maxWaitMillis;
		if(period!=null)this.priod= Long.valueOf(period);
	}

	public String initTargetRedisSources(String jsonStr){
		try {
			if(!lock.tryLock()){
				return ReturnsUtil.returnException("获取锁异常：",new BaseException("存在前置操作，请销毁或者等待!")).toString();
			}
			JSONObject json = JSONObject.fromObject(jsonStr);
			Integer poolNum = JsonUtil.getNotNullIntFromJSON(json,"poolNum","从入参中获取poolNum");
			destory();
			this.broken = false;
			for (int i = 0; i < poolNum; i++) {
				if(this.broken){
					throw new BaseException("当前初始化任务已经被中断...");
				}
				initRedisClient();
			}
		}catch (Exception e){
			return ReturnsUtil.returnException("初始化连接池失败：", e).toString();
		}finally {
			try {
				lock.unlock();
			}catch (Exception e){

			}
		}
		return ReturnsUtil.returnSuccess("{\"curPoolNum\":\""+targetRedisSources.size()+"\"}").toString();
	}
	/**
	 * 初始化接入机连接池
	 */
	public  String initRedisClient(){
		try {
			List<HostAndPort> hostAndPortList = new ArrayList();
			String[] server= servers.split(",");
			if (null!=server){
				for (int i = 0; i < server.length; i++) {
					String[] ip = server[i].split(":");
					HostAndPort host = new HostAndPort(ip[0], Integer.parseInt(ip[1]));
					hostAndPortList.add(host);
				}
			}
			GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
			poolConfig.setMaxIdle(maxIdle); //最大空闲连接数 30
			poolConfig.setMaxTotal(maxActive); // 最大连接数（空闲+使用中） 200
			poolConfig.setMinIdle(minIdle); //保持的最小空闲连接数 3
			poolConfig.setMaxWaitMillis(maxWaitMillis); //3000

			CtgJedisPoolConfig config = new CtgJedisPoolConfig(hostAndPortList);

			config.setDatabase(4970).setPassword(passWord).setPoolConfig(poolConfig)
					.setPeriod(this.priod).setMonitorTimeout(100);
			CtgJedisPool ctgJedisPool = new CtgJedisPool(config);
			targetRedisSources.add(ctgJedisPool);
		}catch (Exception e){
			return  ReturnsUtil.returnException("初始化连接池失败：", e).toString();
		}
		return ReturnsUtil.returnSuccess("{\"curPoolNum\":\""+targetRedisSources.size()+"\"}").toString();
	}
	/**
	 * 初始化常用连接池
	 */
	public String initResolvedRedisSources(String jsonString){
		try {
			JSONObject json = JSONObject.fromObject(jsonString);
			String errorMsg ="初始化常用连接池获取参数时";
			Integer resolveRedisNum =JsonUtil.getNotNullIntFromJSON(json, "resolveRedisNum",errorMsg);
			Assert.notNull(targetRedisSources, "全量接入机连接池未初始化");
			Assert.state(resolveRedisNum<=targetRedisSources.size(), "常量连接池设置过大,当前全量连接池大小为："+targetRedisSources.size());
			if(resolvedRedisSources == null) resolvedRedisSources = new ArrayList<CtgJedisPool>();
			resolvedRedisSources.clear();
			resolvedRedisSources.addAll(createRandomList(targetRedisSources,resolveRedisNum));
		} catch (Exception e) {
			return ReturnsUtil.returnException("初始化常用连接池失败：", e).toString();
		}
		return ReturnsUtil.returnSuccess("{\"resolveRedisNum\":\""+resolvedRedisSources.size()+"\"}").toString();
	}
	
	/**
	 * 缓存操作接口
	 */
	/**
	 * 入参样例1:{"mode":"G","key":"ds1_120013765787_key_ds1120013765787_soNbr"}
	 * 入参样例2:{"mode":"CL","key":"ds1_120013765787_key_ds1120013765787_soNbr"}
	 * 入参样例3:{"mode":"P","key":"queryUninstalled17728192237120001041207","clz":"java.lang.String","obj":"F"}
	 * @param keyStr
	 * @author liubo
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String provCacheProxy(String keyStr){
		JSONObject value =null;
		try {
			JSONObject keyJson = JSONObject.fromObject(keyStr);
			String mode = JsonUtil.getNotNullStringFromJSON(keyJson,"mode","从入参中获取mode时");
			String key = JsonUtil.getNotNullStringFromJSON(keyJson,"key","从入参中获取key时");
			String[] items = mode.split(",");
			Random rand = new Random();
			CtgJedisPool jedisPool = resolvedRedisSources.get(rand.nextInt(resolvedRedisSources.size()));
			ProxyJedis jedis  = null;
			for (int i = 0; i < items.length; i++) {		
				if("P".equals(items[i])){
					String obj = JsonUtil.getNotNullStringFromJSON(keyJson,"obj","从入参中获取obj时");
					jedis= jedisPool.getResource();
					jedis.set(key, obj);
					jedis.close();
				}
				if("CL".equals(items[i])){
					jedis= jedisPool.getResource();
					jedis.del(key);
					jedis.close();
				}
				if("G".equals(items[i])){
					jedis= jedisPool.getResource();
					String v = jedis.get(key);	
					value = new JSONObject();
					value.put("value", v);
					jedis.close();
				}
			}

		} catch (Exception e) {
			return ReturnsUtil.returnException("远程获取缓存代理服务异常", e).toString();
		}
		return ReturnsUtil.returnSuccess(value).toString();
	}
	
	/**
	 * 销毁连接池
	 */
	public String destory(){
		try{
			this.broken = true;
			Thread.sleep(1000);
			if(targetRedisSources !=null&&targetRedisSources.size()>0){
				if(resolvedRedisSources!=null&&resolvedRedisSources.size()>0)resolvedRedisSources.clear();
				for (int i = 0; i < targetRedisSources.size(); i++) {
					CtgJedisPool redisPool = targetRedisSources.get(i);
					redisPool.close();
				}
				targetRedisSources.clear();
			}	
		}catch(Exception e){
			return ReturnsUtil.returnException("销毁接入机连接池", e).toString();
		}
		return ReturnsUtil.returnSuccess(null).toString();
	}
	
	/**
	 * 获取ctgJedisPool初始化配置参数信息
	 * @return
	 */
	public String queryCtgCacheInitParams(){
		JSONObject json = new JSONObject();
		json.put("targetRedisSources", targetRedisSources!=null?targetRedisSources.size():0);
		json.put("resolvedRedisSources", resolvedRedisSources!=null?resolvedRedisSources.size():0);
		json.put("servers", servers);
		json.put("passWord", passWord);
		json.put("maxActive", maxActive);
		json.put("maxIdle", maxIdle);
		json.put("database", database);
		json.put("minIdle", minIdle);
		json.put("maxWaitMillis", maxWaitMillis);
		json.put("period", this.priod);
		json.put("broken",broken);

		JSONArray targetRedisSourcesArray = new JSONArray();
		if(targetRedisSources!=null){
			for (int i=0;i<targetRedisSources.size();i++){
				CtgJedisPool poool = (CtgJedisPool) targetRedisSources.get(i);
				JSONObject poolParam = new JSONObject();
				//活动连接数
				poolParam.put("numActive",poool.getNumActive());
				//空闲连接数
				poolParam.put("numIdle",poool.getNumIdle());
				poolParam.put("numWaiters",poool.getNumWaiters());
				targetRedisSourcesArray.add(poolParam);
			}
			json.put("targetRedisSourcesArray",targetRedisSourcesArray);
		}

		return json.toString();
	}
	
	  /**从list中随机抽取元素 
     * @return   
     * @Title: createRandomList  
     * @Description: TODO 
     * @param list 
     * @return void
     * @throws  
     */   
    private static List createRandomList(List list, int n) {  
        // TODO Auto-generated method stub  
        Map map = new HashMap();  
        List listNew = new ArrayList();  
        if(list.size()<=n){  
            return list;  
        }else{  
            while(map.size()<n){  
                int random = (int) (Math.random() * list.size());  
                if (!map.containsKey(random)) {  
                    map.put(random, "");  
//                    System.out.println(random+"==========="+list.get(random));
                    listNew.add(list.get(random));  
                }  
            }  
            return listNew;  
        }  
    }  
  

	
	public static void main(String[] args){
		Random rand = new Random();
		for (int i = 0; i < 20; i++) {
			 int random = (int) (Math.random() * 20);
			 System.out.println(random);
//			 System.out.println(rand.nextInt(20)+1);
		}
	}
}


