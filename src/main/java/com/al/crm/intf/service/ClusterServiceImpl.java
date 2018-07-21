package com.al.crm.intf.service;

import com.al.common.exception.BaseException;
import com.al.crm.utils.JsonUtil;
import com.al.crm.utils.ReturnsUtil;
import com.ctg.itrdc.cache.pool.CtgJedisPool;
import com.ctg.itrdc.cache.pool.CtgJedisPoolConfig;
import com.ctg.itrdc.cache.pool.ProxyJedis;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

@Service("ctgCacheService")
public class ClusterServiceImpl {
	//全量连接池表(需要线程安全)
	private  List<JedisCluster> targetRedisSources = new CopyOnWriteArrayList<JedisCluster>();
	//常用连接池表(需要线程安全)
	private List<JedisCluster> resolvedRedisSources;

	private String servers ="10.128.91.222:26379,10.128.91.222:26380,10.128.91.222:26381" +
			",10.128.91.225:26382,10.128.91.225:26383,10.128.91.225:26384";
	
	private JedisPoolConfig config;
	
	private Integer maxActive =300;
	
	private Integer maxIdle =300;

	private Integer minIdle=30;
	
	private Integer maxWaitMillis=3000;

	private ReentrantLock lock = new ReentrantLock();

	private Boolean broken =false;

	private String testWhileIdle = "false";

	private String testOnBorrow ="false";

	private String testOnReturn ="false";

	private String timeBetweenEvictionRunsMillis =null;

	private String minEvictableIdleTimeMillis =null;
	
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
			Integer maxActive = JsonUtil.getIntByKey(json, "maxActive");
			Integer maxIdle = JsonUtil.getIntByKey(json, "maxIdle");
			Integer minIdle = JsonUtil.getIntByKey(json, "minIdle");
			Integer maxWaitMillis = JsonUtil.getIntByKey(json, "maxWaitMillis");
			String testWhileIdle =JsonUtil.getStringFromJSON(json, "testWhileIdle", errorMsg);
			String testOnBorrow =JsonUtil.getStringFromJSON(json, "testOnBorrow", errorMsg);
			String testOnReturn =JsonUtil.getStringFromJSON(json, "testOnReturn", errorMsg);
			String timeBetweenEvictionRunsMillis =JsonUtil.getStringFromJSON(json, "timeBetweenEvictionRunsMillis", errorMsg);
			String minEvictableIdleTimeMillis =JsonUtil.getStringFromJSON(json, "minEvictableIdleTimeMillis", errorMsg);

			this.initParams(servers,maxActive,maxIdle,minIdle,maxWaitMillis,testWhileIdle,testOnBorrow,testOnReturn,timeBetweenEvictionRunsMillis,minEvictableIdleTimeMillis);
		} catch (Exception e) {
			return ReturnsUtil.returnException("初始化接入机连接池获取参数时", e).toString();
		}
		return queryCtgCacheInitParams();
		
	}

	private JedisPoolConfig getPoolConfig() {
		//GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config = new JedisPoolConfig();
		//最大空闲连接数, 默认8个
		if (maxIdle != null) {
			config.setMaxIdle(maxIdle);
		} else {
			config.setMaxIdle(16);
		}
		//最小空闲连接数, 默认0个
		if (minIdle != null) {
			config.setMinIdle(minIdle);
		} else {
			config.setMinIdle(1);
		}
		//控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；如果赋值为-1，则表示不限制；
		if (maxActive != null) {
			config.setMaxTotal(maxActive);
		} else {
			config.setMaxTotal(16);
		}
		//高版本maxActive->maxTotal
		if (maxActive != null) {
			config.setMaxTotal(maxActive);
		}
//        else {
//            config.setMaxTotal(8);
//        }
		//在空闲时检查有效性, 默认false
		if (testWhileIdle != null) {
			config.setTestWhileIdle(Boolean.parseBoolean(testWhileIdle));
		}
		//连接时检查有效性, 默认false
		if (testOnBorrow != null) {
			config.setTestOnBorrow(Boolean.parseBoolean(testOnBorrow));
		}
		//在return给pool时，是否提前进行validate操作
		if (testOnReturn != null) {
			config.setTestOnReturn(Boolean.parseBoolean(testOnReturn));
		}
		//逐出扫描的时间间隔(毫秒) 如果为负数,则不运行逐出线程, 默认-1
		if (timeBetweenEvictionRunsMillis !=null ) {
			config.setTimeBetweenEvictionRunsMillis(Long.parseLong(timeBetweenEvictionRunsMillis));
		} else {
			config.setTimeBetweenEvictionRunsMillis(600000);
		}
		//表示一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
		if (minEvictableIdleTimeMillis!=null) {
			config.setMinEvictableIdleTimeMillis(Long.parseLong(minEvictableIdleTimeMillis));
		} else {
			//逐出连接的最小空闲时间 默认1800000毫秒(30分钟)
			config.setMinEvictableIdleTimeMillis(1800000);
		}

		//连接耗尽时是否阻塞, false报异常,ture阻塞直到超时, 默认true
		config.setBlockWhenExhausted(true);
		//获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常, 小于零:阻塞不确定的时间,  默认-1
		config.setMaxWaitMillis(30000);
		//每次逐出检查时 逐出的最大数目 如果为负数就是 : 1/abs(n), 默认3
		config.setNumTestsPerEvictionRun(5);
		//对象空闲多久后逐出, 当空闲时间>该值 且 空闲连接>最大空闲数 时直接逐出,不再根据MinEvictableIdleTimeMillis判断  (默认逐出策略)
		config.setSoftMinEvictableIdleTimeMillis(1800000);
		return  config;
	}
	
	/**
	 * 设置参数
	 * @return 
	 */
	private synchronized void initParams(String servers, Integer maxActive, Integer maxIdle, Integer minIdle
			, Integer maxWaitMillis, String testWhileIdle, String testOnBorrow,
										 String testOnReturn, String timeBetweenEvictionRunsMillis, String minEvictableIdleTimeMillis){
		destory();
		//1.设置初始化参数
		if(StringUtils.isNotBlank(servers))this.servers=servers;
		if(maxActive!=null)this.maxActive=maxActive;
		if(maxIdle!=null)this.maxIdle=maxIdle;
		if(minIdle!=null)this.minIdle=minIdle;
		if(maxWaitMillis!=null)this.maxWaitMillis=maxWaitMillis;
		if(testWhileIdle!=null)this.testWhileIdle=testWhileIdle;
		if(testOnBorrow!=null)this.testOnBorrow=testOnBorrow;
		if(testOnReturn!=null)this.testOnReturn=testOnReturn;
		if(timeBetweenEvictionRunsMillis!=null)this.timeBetweenEvictionRunsMillis=timeBetweenEvictionRunsMillis;
		if(minEvictableIdleTimeMillis!=null)this.minEvictableIdleTimeMillis=minEvictableIdleTimeMillis;
//		getPoolConfig();
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
			Set<HostAndPort> jedisClusterNodes = new HashSet<>();
			String[] server= servers.split(",");
			if (null!=server){
				for (int i = 0; i < server.length; i++) {
					String[] ip = server[i].split(":");
					HostAndPort host = new HostAndPort(ip[0], Integer.parseInt(ip[1]));
					jedisClusterNodes.add(host);
				}
			}
//			GenericObjectPoolConfig poolConfig =getPoolConfig();
			int connectionTimeout = 30000;
			int soTimeout = connectionTimeout;
			int maxAttempts = 5;
			JedisCluster cluster = new JedisCluster(jedisClusterNodes, connectionTimeout,
						soTimeout, maxAttempts, getPoolConfig());
			targetRedisSources.add(cluster);
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
			if(resolvedRedisSources == null) resolvedRedisSources = new ArrayList<JedisCluster>();
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
			JedisCluster jedisCluster = resolvedRedisSources.get(rand.nextInt(resolvedRedisSources.size()));
			for (int i = 0; i < items.length; i++) {
				if("P".equals(items[i])){
					String obj = JsonUtil.getNotNullStringFromJSON(keyJson,"obj","从入参中获取obj时");
					jedisCluster.set(key, obj);
				}
				if("CL".equals(items[i])){
					jedisCluster.del(key);
				}
				if("G".equals(items[i])){
					String v = jedisCluster.get(key);
					value = new JSONObject();
					value.put("value", v);
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
					JedisCluster cluster = targetRedisSources.get(i);
					cluster.close();
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
		json.put("config",JSONObject.fromObject(config));

		JSONArray targetRedisSourcesArray = new JSONArray();
		if(targetRedisSources!=null){
			for (int i=0;i<targetRedisSources.size();i++){
				JedisCluster cluster = (JedisCluster) targetRedisSources.get(i);
				Map<String, JedisPool> pools = cluster.getClusterNodes();
				Integer numActive = 0;
				Integer numIdle = 0;
				Integer numWaiters = 0;
				Long maxBorrowWaitTimeMillis = 0l;
				Long TmeanBorrowWaitTimeMillis = 0l;

				for (Map.Entry entry:pools.entrySet() ) {
					JedisPool pool = (JedisPool) entry.getValue();
					numActive  = numActive+pool.getNumActive();
					numIdle = numIdle + pool.getNumIdle();
					numWaiters = numWaiters + pool.getNumWaiters();
					maxBorrowWaitTimeMillis= maxBorrowWaitTimeMillis>pool.getMaxBorrowWaitTimeMillis()?maxBorrowWaitTimeMillis:pool.getMaxBorrowWaitTimeMillis();
					TmeanBorrowWaitTimeMillis  = TmeanBorrowWaitTimeMillis+pool.getMeanBorrowWaitTimeMillis();
				}
				JSONObject poolParam = new JSONObject();
				//活动连接数
				poolParam.put("numActive",numActive);
				//空闲连接数
				poolParam.put("numIdle",numIdle);
				poolParam.put("numWaiters",numWaiters);
				poolParam.put("numWaiters",numWaiters);
				poolParam.put("maxBorrowWaitTimeMillis",maxBorrowWaitTimeMillis);
				poolParam.put("meanBorrowWaitTimeMillis",TmeanBorrowWaitTimeMillis/pools.size());
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


