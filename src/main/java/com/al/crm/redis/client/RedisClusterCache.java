package com.al.crm.redis.client;

import com.al.crm.nosql.cache.CacheException;
import com.al.crm.nosql.cache.ICache;
import com.al.crm.nosql.cache.IRedisFix;
import com.al.crm.nosql.cache.KeyCount;
import com.al.crm.nosql.cache.impl.AbstractCache;
import com.al.crm.nosql.cache.impl.RedisCache;
import com.al.crm.nosql.cache.util.Utils;
import com.al.crm.nosql.cache.util.lock.DistLock;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisClusterException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shish on 2017/8/17.
 * 实现jedis对接集群
 */
public class RedisClusterCache implements IRedisFix {

    private static Logger log = LoggerFactory.getLogger(RedisClusterCache.class);

    private JedisCluster cluster = null;


    private static boolean isKryoserialize = false;

    /**
     * 是否要强制超时
     */
    private int allTimeOut = 0;
    private boolean istimeout = false;

    protected String serializerType = "json";

    private final String KRYO = "kryo";

    private String pwd = "";

    private String redisUrls = "";

    @Override
    public String toString() {
        return "RedisClusterCache{" +
                "client=" + cluster +
                ", serializerType='" + serializerType + '\'' +
                ", redisUrls='" + redisUrls + '\'' +
                '}';
    }

    public RedisClusterCache(Properties props) {

        if (props.containsKey("serializer.type")) {
            serializerType = props.getProperty("serializer.type");
        }

        if (props.containsKey("alltimeout")) {
            istimeout = true;
            allTimeOut = Integer.valueOf(props.getProperty("alltimeout"));
        }

        isKryoserialize = KRYO.equals(serializerType);

        this.redisUrls = props.getProperty("redis.url");
        int connectionTimeout = 30000;
        int soTimeout = connectionTimeout;

        int maxAttempts = 5;
        GenericObjectPoolConfig poolConfig = getPoolConfig(props);
        Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
        //Jedis Cluster will attempt to discover client nodes automatically
        String[] urls = StringUtils.split(redisUrls, ",");
        for (int i = 0; i < urls.length; i++) {
            jedisClusterNodes.add(resolver(urls[i], this));
        }
        if(StringUtils.isBlank(this.pwd)){
            cluster = new JedisCluster(jedisClusterNodes, connectionTimeout,
                    soTimeout, maxAttempts, poolConfig);
        }else{
            cluster = new JedisCluster(jedisClusterNodes, connectionTimeout,
                    soTimeout, maxAttempts, this.pwd, poolConfig);
        }
    }

    public static GenericObjectPoolConfig getPoolConfig(Properties props) {
        //GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        JedisPoolConfig config = new JedisPoolConfig();
        //最大空闲连接数, 默认8个
        if (props.getProperty("redis.maxIdle") != null) {
            config.setMaxIdle(Integer.parseInt(props.getProperty("redis.maxIdle")));
        } else {
            config.setMaxIdle(16);
        }
        //最小空闲连接数, 默认0个
        if (props.getProperty("redis.minIdle") != null) {
            config.setMinIdle(Integer.parseInt(props.getProperty("redis.minIdle")));
        } else {
            config.setMinIdle(1);
        }
        //控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；如果赋值为-1，则表示不限制；
        if (props.getProperty("redis.maxActive") != null) {
            config.setMaxTotal(Integer.parseInt(props.getProperty("redis.maxActive")));
        } else {
            config.setMaxTotal(16);
        }
        //高版本maxActive->maxTotal
        if (props.getProperty("redis.maxTotal") != null) {
            config.setMaxTotal(Integer.parseInt(props.getProperty("redis.maxTotal")));
        }
//        else {
//            config.setMaxTotal(8);
//        }
        //在空闲时检查有效性, 默认false
        if (props.getProperty("redis.testWhileIdle") != null) {
            config.setTestWhileIdle(Boolean.parseBoolean(props.getProperty("redis.testWhileIdle")));
        }
        //连接时检查有效性, 默认false
        if (props.getProperty("redis.testOnBorrow") != null) {
            config.setTestOnBorrow(Boolean.parseBoolean(props.getProperty("redis.testOnBorrow")));
        }
        //在return给pool时，是否提前进行validate操作
        if (props.getProperty("redis.testOnReturn") != null) {
            config.setTestOnReturn(Boolean.parseBoolean(props.getProperty("redis.testOnReturn")));
        }
        //逐出扫描的时间间隔(毫秒) 如果为负数,则不运行逐出线程, 默认-1
        if (props.containsKey("redis.timeBetweenEvictionRunsMillis")) {
            config.setTimeBetweenEvictionRunsMillis(Long.parseLong(props.getProperty("redis.timeBetweenEvictionRunsMillis")));
        } else {
            config.setTimeBetweenEvictionRunsMillis(600000);
        }
        //表示一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
        if (props.containsKey("redis.minEvictableIdleTimeMillis")) {
            config.setMinEvictableIdleTimeMillis(Long.parseLong(props.getProperty("redis.minEvictableIdleTimeMillis")));
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

        return config;
    }

    /**
     * 解析 redis://:pwd@127.0.0.1:6379/0 地址
     *
     * @param url
     * @return
     */
    public static HostAndPort resolver(String url, RedisClusterCache rc) throws RuntimeException {
        String regEx = "redis:\\/\\/:(.*)@(.+):(\\d+)\\/\\d+";
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regEx);
        // 忽略大小写的写法
        // Pattern pat = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        // 字符串是否与正则表达式相匹配
        boolean rs = matcher.matches();
        if (rs) {
            rc.setPwd(matcher.group(1));
            String addr = matcher.group(2);
            int port = Integer.parseInt(matcher.group(3));
            log.debug("集群地址:{}:{}", addr, port);
            return new HostAndPort(addr, port);
        }
        throw new RuntimeException("不正确的redis配置地址");
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        if (StringUtils.isNotBlank(this.pwd)) {
            if (this.pwd.equals(pwd)) {
                return;
            } else {
                log.error("多个redis地址密码不统一[{},{}]", this.pwd, pwd);
                throw new RuntimeException("多个redis地址密码不统一");
            }
        }
        this.pwd = pwd;
    }

    public <T> T get(String key, Class<T> clazz) {
        return get(null, key, clazz);
    }

    public <T> T get(String dir, String key, Class<T> clazz) {
        Object value = cluster.get(AbstractCache.genCacheKey(dir, key));
        if (value == null) return null;

        try {
            return Utils.getObjFromStr((String) value, clazz);
        } catch (IOException e) {
            throw new CacheException(e);
        }
    }

    public Object get(String key) {
        return get(null, key);
        //return client.get(key);
    }

    public byte[] get(byte[] key) {
        return cluster.get(key);
    }

    public Object get(String dir, String key) {
        String wpkey = AbstractCache.genCacheKey(dir, key);

        Object value = null;
        long beginTime = System.currentTimeMillis();
        if (isKryoserialize) {
            value = cluster.get(wpkey.getBytes());
        } else {
            value = cluster.get(wpkey);
        }
        long costTime = System.currentTimeMillis() - beginTime;
        if (costTime >= 200) {
            log.warn("获取缓存数据耗费{}毫秒(不含反序列耗时)，超过{}ms,key=" + wpkey, costTime, 200);
            log.warn("get key[" + wpkey + "] value[" + value + "]...");
        }

        if (value == null) return null;

        log.info("rediscluster get key[" + key + "]  value[" + value + "]");
        //System.out.println("rediscluster get key[" + key + "]  value[" + value + "]");

        beginTime = System.currentTimeMillis();
        try {
//            if (isKryoserialize && value instanceof byte[]) {
//                return KyroSerializer.getObjectFromByte((byte[]) value);
//            } else
                if ((value instanceof String) && Utils.isPossibleJSON((String) value)) {
                return Utils.getObjFromJson((String) value);
            }
        } catch (IOException e) {
            throw new CacheException(e);
        } catch (ClassNotFoundException e) {
            throw new CacheException(e);
        } finally {
            costTime = System.currentTimeMillis() - beginTime;
            if (costTime >= 100) {
                log.warn("缓存数据反序列化对象耗费{}毫秒，超过{}ms,key=" + wpkey + ",value =" + value, costTime, 100);
            }
        }

        return value;
    }

    @Deprecated
    public Object getRawData(String key) {
        return null;
    }

    @Deprecated
    public Object getRawData(String dir, String key) {
        return null;
    }

    public void put(String key, Object object, int expireTime, int maxSizePerObj) {
        put(null, key, object, expireTime, maxSizePerObj);
    }

    public void put(String dir, String key, Object object, int expireTime, int maxSizePerObj) {
        if (expireTime < 0 && istimeout) {
            expireTime = allTimeOut;
        }
        if (object == null) return;
        long beginSerialize = System.currentTimeMillis();
        Object value = AbstractCache.toStoreValue(object, maxSizePerObj);
        long endSerialize = System.currentTimeMillis();
        if (value == null) return;

        long beginTime = System.currentTimeMillis();
        String wpkey = AbstractCache.genCacheKey(dir, key);
        try {
            if (value instanceof byte[]) {
                if (expireTime > 0) {
                    cluster.setex(wpkey.getBytes(), expireTime, (byte[]) value);
                } else {
                    cluster.set(wpkey.getBytes(), (byte[]) value);
                }
            } else {
                if (expireTime > 0) {
                    cluster.setex(wpkey, expireTime, String.valueOf(value));
                } else {
                    cluster.set(wpkey, String.valueOf(value));
                }
                log.info("rediscluster key:{} , value: {}", wpkey, String.valueOf(value));
                //System.out.println("rediscluster key:" + wpkey + " , value: " + String.valueOf(value));
            }
        } catch (Exception e) {
            log.error("{}", e);
            e.printStackTrace();
        } finally {
            long costTime = System.currentTimeMillis() - beginTime;
            if (costTime >= 200) {
                long serializeCostTime = endSerialize - beginSerialize;
                log.warn("存储缓存数据对象耗费{}毫秒，其中序列化耗费{}毫秒，超过{}ms,key=" + wpkey + ",value =" + value, new Object[]{costTime, serializeCostTime, 200});
            }
        }
        value = null;
    }

    public void put(String key, Object object) {
        put(key, object, -1, -1);
    }

    public void put(String dir, String key, Object object) {
        put(dir, key, object, -1, -1);
    }

    public void put(String key, Object object, int expireTime) {
        put(key, object, expireTime, -1);
    }

    public void put(String dir, String key, Object object, int expireTime) {
        put(dir, key, object, expireTime, -1);
    }

    public boolean putIfNotExists(String key, Object object) {
        if (object == null) return false;

        Object value = AbstractCache.toStoreValue(object, -1);
        if (value == null) return false;

        return setnx(key, value.toString()) == 1 ? true : false;
    }

    public boolean remove(String key) {
        return remove(null, key);
    }

    public boolean remove(String dir, String key) {
        return cluster.del(AbstractCache.genCacheKey(dir, key)) > 0 ? true : false;
    }

    public void removeAll() {
        cluster.flushAll();
    }

    public void setExpireTime(String key, int expireTime) {
        cluster.expire(key, expireTime);
    }

    public void setExpireTime(String dir, String key, int expireTime) {
        cluster.expire(AbstractCache.genCacheKey(dir, key), expireTime);
    }

    public int getExpireTime(String key) {
        return cluster.ttl(key).intValue();
    }

    public int getExpireTime(String dir, String key) {
        return cluster.ttl(AbstractCache.genCacheKey(dir, key)).intValue();
    }

    public long incr(String key) {
        return incr(key, 1);
    }

    public long incr(String dir, String key) {
        return incr(dir, key, 1);
    }

    public long incr(String key, long value) {
        return cluster.zincrby(RedisCache.DEFAULT_KEY_FOR_COUNT, value, key).longValue();
    }

    public long incr(String dir, String key, long value) {
        return cluster.zincrby(dir, value, key).longValue();
    }

    public long decr(String key) {
        return decr(key, 1);
    }

    public long decr(String dir, String key) {
        return decr(dir, key, 1);
    }

    public long decr(String key, long value) {
        return decr(RedisCache.DEFAULT_KEY_FOR_COUNT, key, value);
    }

    public long decr(String dir, String key, long value) {
        return cluster.zincrby(dir, value * (-1), key).longValue();
    }

    public int getMaxKeyLen() {
        return -1;
    }

    public List<String> listKeys(String dir, List<String> excludeSubDirs, String keyWord, boolean refresh, int offset, int limit) {
        throw new JedisClusterException("No support this command to Redis Cluster.");
        //return null;
    }

    public List<String> listKeys(String dir, List<String> excludeSubDirs, String keyWord, int offset, int limit) {
        throw new JedisClusterException("No support this command to Redis Cluster.");
    }

    public long getKeyCount(String dir, String key) {
        Double cnt = cluster.zscore(dir, key);
        if (cnt == null) {
            return -1;
        } else {
            return cnt.longValue();
        }
    }

    public long getKeyCount(String key) {
        return getKeyCount(RedisCache.DEFAULT_KEY_FOR_COUNT, key);
    }

    public void removeKeyCount(String dir, String key) {
        cluster.zrem(dir, key);
    }

    public void removeKeyCount(String key) {
        removeKeyCount(RedisCache.DEFAULT_KEY_FOR_COUNT, key);
    }

    public void sadd(String key, String member) {
        cluster.sadd(key, member);
    }

    public void sadd(String key, List<String> members) {
        cluster.sadd(key, members.toArray(new String[members.size()]));
    }

    public void srem(String key, String member) {
        cluster.srem(key, member);
    }

    public Collection<String> getMembers(String key) {
        return cluster.smembers(key);
    }

    public List<KeyCount> listTopCount(String dir, int sortFlag, int offset, int amount) {
        List<KeyCount> list = new LinkedList<KeyCount>();

        Set set = sortFlag == 0 ?
                cluster.zrangeWithScores(dir, offset * amount, amount < 0 ? -1 : (offset + 1) * amount - 1)
                : cluster.zrevrangeWithScores(dir, offset * amount, amount < 0 ? -1 : (offset + 1) * amount - 1);

        for (Object obj : set) {
            Tuple one = (Tuple) obj;
            KeyCount kc = new KeyCount();
            kc.setKey(one.getElement());
            Double cnt = one.getScore();
            kc.setCount(cnt.longValue());
            list.add(kc);
        }

        return list;
    }

    public List<KeyCount> listTopCount(String dir, int sortFlag, boolean refresh, int offset, int amount) {
        return listTopCount(dir, sortFlag, offset, amount);
    }

    public void batchRemove(String dir, List<String> keyList) {
        if (keyList == null || keyList.size() == 0) {
            return;
        }
        for (String key : keyList) {
            remove(dir, key);
        }

    }

    public String[] getServers() {
        return new String[0];
    }

    public void destroy() {

    }

    public boolean isAlive(String key) {
        return true;
    }

    public boolean isAlive(String dir, String key) {
        return true;
    }

    public String getCacheBackend() {
        return ICache.CACHE_BACKEND_REDIS_CLUSTER;
    }

    public ICache getCurrentCache() {
        return null;
    }

    public boolean delkey(String key) {
        return cluster.del(key) > 0L ? true : false;
    }

    public long ttl(String key) {
        return cluster.ttl(key);
    }

    public long pttl(String key) {
        return cluster.ttl(key);
    }

    public long setnx(String key, String value) {
        return cluster.setnx(key, value);
    }

    public Long expire(byte[] key, int seconds) {
        return cluster.expire(key, seconds);
    }

    public boolean exists(String key) {
        return cluster.exists(key);
    }

    public Long hset(String hash, String key, String value) {
        return cluster.hset(hash, key, value);
    }

    public Long hsetnx(String hash, String key, String value) {
        return cluster.hsetnx(hash, key, value);
    }

    public Long hset(byte[] hash, byte[] key, byte[] value) {
        return cluster.hset(hash, key, value);
    }

    public Long hsetPojo(String hash, String key, Object pojo) throws IOException {
        String json = Utils.toCacheJson(pojo);
        return hset(hash, key, json);
    }

    public Object hgetPojo(String hash, String key) {
        String json = hget(hash, key);
        try {
            return Utils.getRealObject(json);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            log.error("jvm中没有找到要实例化的类定义:{}", json);
            e.printStackTrace();
            return null;
        }
    }

    public String hget(String hash, String key) {
        return cluster.hget(hash, key);
    }

    public byte[] hget(byte[] hash, byte[] key) {
        return cluster.hget(hash, key);
    }

    public Boolean hexists(byte[] hash, byte[] key) {
        return cluster.hexists(hash, key);
    }

    public Boolean hexists(String hash, String key) {
        return cluster.hexists(hash, key);
    }

    public Long hincrby(byte[] hash, byte[] key, Long value) {
        return cluster.hincrBy(hash, key, value);
    }

    public Long hincrby(String hash, String key, Long value) {
        return cluster.hincrBy(hash, key, value);
    }

    public Long hlen(byte[] hash) {
        return cluster.hlen(hash);
    }

    public Long hlen(String hash) {
        return cluster.hlen(hash);
    }

    public Long hdel(byte[] hash, byte[] key) {
        return cluster.hdel(hash, key);
    }

    public Long hdel(String hash, String key) {
        return cluster.hdel(hash, key);
    }

    public Set<String> hkeys(byte[] hash) {
        return cluster.hkeys(new String(hash));
    }

    public Set<String> hkeys(String hash) {
        return cluster.hkeys(hash);
    }

    public List<byte[]> hvals(byte[] hash) {
        return (List<byte[]>) cluster.hvals(hash);
    }

    public List<byte[]> hvals(String hash) {
        return (List<byte[]>) cluster.hvals(hash.getBytes());
    }

    public Map<byte[], byte[]> hgetall(byte[] hash) {
        return cluster.hgetAll(hash);
    }

    public Map<String, byte[]> hgetall(String hash) {
        Map<byte[], byte[]> map = hgetall(hash.getBytes());
        Map<String, byte[]> res = new HashMap<String, byte[]>();
        Set<byte[]> set = map.keySet();
        for (byte[] bs : set) {
            res.put(new String(bs), map.get(bs));
        }
        return res;
    }

    public Map<String, Object> hgetallPojo(String hash) {
        Map<byte[], byte[]> map = hgetall(hash.getBytes());
        Map<String, Object> res = new HashMap<String, Object>();
        Set<byte[]> set = map.keySet();
        for (byte[] bs : set) {
            String json = new String(map.get(bs));
            try {
                res.put(new String(bs), Utils.getRealObject(json));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                log.error("jvm中没有找到要实例化的类定义:{}", json);
                e.printStackTrace();
            }
        }
        return res;
    }

    public Long lpush(String key, String... value) {
        return cluster.lpush(key, value);
    }

    public Long lpush(byte[] key, byte[]... value) {
        return cluster.lpush(key, value);
    }

    public Long rpush(String key, String... value) {
        return cluster.rpush(key, value);
    }

    public Long rpush(byte[] key, byte[]... value) {
        return cluster.rpush(key, value);
    }

    public Long rpush(String key, Object... values) {
        String[] args = toObjectArray(values);
        return cluster.rpush(key, args);
    }

    private String[] toObjectArray(Object[] values) {
        String[] args = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            try {
                String value = null;
                if (Utils.isBasicType(values[i].getClass())) {
                    value = values[i].toString();
                } else {
                    value = (String) AbstractCache.buildCacheObject(values[i]);
                }
                args[i] = value;
            } catch (IOException e) {
                throw new CacheException(e);
            }
        }
        return args;
    }

    public Long lpush(String key, Object... values) {
        String[] args = toObjectArray(values);
        return cluster.lpush(key, args);
    }

    public Long linsert(String key, BinaryClient.LIST_POSITION position, String pivot, String value) {
        return cluster.linsert(key, position, pivot, value);
    }

    public Long linsert(byte[] key, BinaryClient.LIST_POSITION position, byte[] pivot, byte[] value) {
        return cluster.linsert(key, position, pivot, value);
    }

    public boolean lset(String key, long index, String value) {
        return "OK".equals(cluster.lset(key, index, value));
    }

    public boolean lset(byte[] key, long index, byte[] value) {
        return "OK".equals(cluster.lset(key, index, value));
    }

    public Long lrem(String key, long count, String value) {
        return cluster.lrem(key, count, value);
    }

    public Long lrem(byte[] key, long count, byte[] value) {
        return cluster.lrem(key, count, value);
    }

    public boolean ltrim(String key, long start, long end) {
        return "OK".equals(cluster.ltrim(key, start, end));
    }

    public boolean ltrim(byte[] key, long start, long end) {
        return "OK".equals(cluster.ltrim(key, start, end));
    }

    public byte[] lpop(byte[] key) {
        return cluster.lpop(key);
    }

    public String lpop_ori(String key) {
        return cluster.lpop(key);
    }

    public String rpop(String key) {
        return cluster.rpop(key);
    }

    public byte[] rpop(byte[] key) {
        return cluster.rpop(key);
    }

    public List<String> blpop(int timeout, String... keys) {
        return cluster.blpop(timeout, keys);
    }

    public List<byte[]> blpop(int timeout, byte[]... keys) {
        return cluster.blpop(timeout, keys);
    }

    public List<String> brpop(int timeout, String... keys) {
        return cluster.brpop(timeout, keys);
    }

    public List<byte[]> brpop(int timeout, byte[]... keys) {
        return cluster.brpop(timeout, keys);
    }

    public String lindex(String key, long index) {
        return cluster.lindex(key, index);
    }

    public byte[] lindex(byte[] key, long index) {
        return cluster.lindex(key, index);
    }

    public long llen(String key) {
        return cluster.llen(key);
    }

    public long llen(byte[] key) {
        return cluster.llen(key);
    }

    public List<String> lrange(String key, long start, long end) {
        return cluster.lrange(key, start, end);
    }

    public List<byte[]> lrange(byte[] key, long start, long end) {
        return cluster.lrange(key, start, end);
    }

    public Boolean sadd(byte[] key, byte[]... members) {
        return cluster.sadd(key, members) > 0L ? true : false;
    }

    public Boolean srem(byte[] key, byte[]... members) {
        return cluster.srem(key, members) > 0L ? true : false;
    }

    public String spop(String key) {
        return cluster.spop(key);
    }

    public byte[] spop(byte[] key) {
        return cluster.spop(key);
    }

    public long scard(String key) {
        return cluster.scard(key);
    }

    public long scard(byte[] key) {
        return cluster.scard(key);
    }

    public Boolean sismember(String key, String member) {
        return cluster.sismember(key, member);
    }

    public Boolean sismember(byte[] key, byte[] member) {
        return cluster.sismember(key, member);
    }

    public String srandmember(String key) {
        return cluster.srandmember(key);
    }

    public byte[] srandmember(byte[] key) {
        return cluster.srandmember(key);
    }

    public Set<String> smembers(String key) {
        return cluster.smembers(key);
    }

    public Set<byte[]> smembers(byte[] key) {
        return cluster.smembers(key);
    }

    public String type(String key) {
        return cluster.type(key);
    }

    public String type(byte[] key) {
        return cluster.type(key);
    }

    public String set(String key, String value, String nxxx, String expx, int time) {
        return cluster.set(key, value, nxxx, expx, time);
    }

    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        return cluster.set(key, value, nxxx, expx, time);
    }

    public String setex(String key, int timeout, String value) {
        return cluster.setex(key, timeout, value);
    }

    public String setex(byte[] key, int timeout, byte[] value) {
        return cluster.setex(key, timeout, value);
    }

    public String set(byte[] key, byte[] value) {
        if (istimeout) {
            return this.setex(key, allTimeOut, value);
        }
        return cluster.set(key, value);
    }

    public String set(String key, String value) {
        if (istimeout) {
            return this.setex(key, allTimeOut, value);
        }
        return cluster.set(key, value);
    }

    public boolean lock(String lockname, String lockvalue, Long timeout, Long expire) {
        return DistLock.lock(this, lockname, lockvalue, timeout, expire);
    }

    public boolean lock(String lockname, String lockvalue, int timeout, int expire) {
        return DistLock.lock(this, lockname, lockvalue, new Long(timeout), new Long(expire));
    }

    public boolean unlock(String lockname, String lockvalue) {
        return DistLock.unlock(this, lockname, lockvalue);
    }

    public boolean unlock(String lockname) {
        return DistLock.unlock(this, lockname, null);
    }

    public boolean relock(String lockname, String lockvalue, Long timeout, Long expire) {
        return DistLock.relock(this, lockname, lockvalue, timeout, expire);
    }

    public boolean relock(String lockname, String lockvalue, int timeout, int expire) {
        return DistLock.relock(this, lockname, lockvalue, new Long(timeout), new Long(expire));
    }

    public boolean unrelock(String lockname, String lockvalue) {
        return DistLock.unrelock(this, lockname, lockvalue);
    }

    public boolean unrelock(String lockname) {
        return DistLock.unrelock(this, lockname, null);
    }

    public String hmset(byte[] key, Map<byte[], byte[]> hash) {
        return cluster.hmset(key, hash);
    }

    public String hmset(String key, Map<String, String> hash) {
        return cluster.hmset(key, hash);
    }

    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        return cluster.hmget(key, fields);
    }

    public List<String> hmget(String key, String... fields) {
        return cluster.hmget(key, fields);
    }


    public ScanResult<String> sscan(String key, String cursor) {
        return cluster.sscan(key, cursor);
    }

    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        return cluster.sscan(key, cursor, params);
    }
}
