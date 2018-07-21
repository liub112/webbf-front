package com.al.crm.redis.client;

import com.al.crm.nosql.cache.*;
import com.al.crm.nosql.cache.impl.AbstractCache;
import com.al.crm.nosql.cache.impl.RedisCache;
import com.al.crm.nosql.cache.impl.RedisDBInfo;
import com.al.crm.nosql.cache.util.Utils;
import com.al.crm.nosql.cache.util.lock.DistLock;
import com.ctg.itrdc.cache.pool.CtgJedisPool;
import com.ctg.itrdc.cache.pool.CtgJedisPoolConfig;
import com.ctg.itrdc.cache.pool.ProxyJedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CtgJedisCache implements IRedisFix {
    private static Logger log = LoggerFactory.getLogger(RedisCache.class);
    private static final int DEFAULT_TIMEOUT = 10;
    public static final String DEFAULT_KEY_FOR_COUNT = "/DEFAULT_COUNT/";
    private int selectDb = 0;
    private static Map<String, Object> poolMap = new HashMap();
    private ShardedJedisPool shardedJedisPool;
    private CtgJedisPool jedisPool;
    private ShardedJedis shardedJedis;
    private Map<String, String> mapServerToServerPort;
    private Map<String, RedisDBInfo> redisDBInfos;
    private JedisPoolConfig redisPoolConfig;
    private Properties props;
    private String redisUrl = null;
    private int timeout = -1;
    private State state;
    private String pwd = "";
    private static final Map<String, Method> JEDIS_METHOD = new HashMap();
    private static final String SCAN_PARAMS_CLASS = "redis.clients.jedis.ScanParams";



    public CtgJedisCache(Properties props) {
        init(props);
        this.state = State.INIT;
    }

    protected void init(Properties props) {
        this.props = props;
        log.info("Redis config info:" + props);
        this.state = State.STARTING;
        this.pwd = props.getProperty("pwd");
        this.redisPoolConfig = this.getPoolConfig(props);
        if (props.getProperty("redis.soTimeout") != null) {
            this.timeout = Integer.parseInt(props.getProperty("redis.soTimeout"));
        }

        this.redisUrl = props.getProperty("redis.url");
        String initTimeSwith = props.getProperty("redis.connWhenStart");
        if ("Y".equals(initTimeSwith)) {
            if (this.redisUrl == null) {
                log.error("Config data  'redis.url' is null,please check it !!");
                throw new CacheException("Config data  'redis.url' is null,please check it !!");
            }

            String[] urls = this.redisUrl.split(",");
            this.jedisPool = this.getJedisPool(this.redisPoolConfig, this.redisUrl, 10);
        }

//        this.buildRedisDbInfo(this.timeout * 1000);
//        this.buildMapServer2ServerPort();
//        this.startConnStatusMgrThread();
        this.state = State.STARTED;
    }

    private int getSelectDb(String redisUrl) {
        URI uri = URI.create(redisUrl);
        int db = Integer.parseInt(uri.getPath().split("/", 2)[1]);
        return db;
    }

    public CtgJedisPool getJedisPool(JedisPoolConfig config, String redisUrls, int timeout) {
        String[] urls = redisUrls.split(",");
        int _timeout = 10000;
        if (timeout > 0) {
            _timeout = timeout * 1000;
        }

        List<HostAndPort> jedisClusterNodes = new ArrayList();
        if (null!=urls){
            for (int i = 0; i < urls.length; i++) {
                String[] ip = urls[i].split(":");
                HostAndPort host = new HostAndPort(ip[0], Integer.parseInt(ip[1]));
                jedisClusterNodes.add(host);
            }
        }

        CtgJedisPoolConfig ctgConfig = new CtgJedisPoolConfig(jedisClusterNodes);
        ctgConfig.setPoolConfig(redisPoolConfig);
        ctgConfig.setDatabase(4970);
        ctgConfig.setPassword(this.pwd);
        ctgConfig.setPeriod(3000);
        ctgConfig.setSoTimeout(30000);
        if (poolMap.containsKey(urls[0])) {
            return (CtgJedisPool)poolMap.get(urls[0]);
        } else {
            if (this.jedisPool == null) {
                this.jedisPool = new CtgJedisPool(ctgConfig);
                poolMap.put(urls[0], this.jedisPool);
            }

            return this.jedisPool;
        }
    }

    /**
     * 解析 redis://:pwd@127.0.0.1:6379/0 地址
     *
     * @param url
     * @return
     */
    public static HostAndPort resolver(String url, CtgJedisCache rc) throws RuntimeException {
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

    public ShardedJedisPool getShardedJedisPool(JedisPoolConfig config, String redisUrls, int timeout) {
        String[] urls = redisUrls.split(",");
        int _timeout = 10000;
        if (timeout > 0) {
            _timeout = timeout * 1000;
        }

        if (urls.length > 1) {
            if (poolMap.containsKey(redisUrls)) {
                return (ShardedJedisPool)poolMap.get(redisUrls);
            }

            List<JedisShardInfo> shards = new ArrayList();
            String[] arr$ = urls;
            int len$ = urls.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String url = arr$[i$];
                JedisShardInfo jsi = new JedisShardInfo(url);
                jsi.setSoTimeout(_timeout);
                shards.add(jsi);
            }

            if (this.shardedJedisPool == null) {
                this.shardedJedisPool = new ShardedJedisPool(config, shards);
                poolMap.put(redisUrls, this.shardedJedisPool);
            }
        }

        return this.shardedJedisPool;
    }


    private void selectDB4Cluster(ShardedJedis shardedJedis, int db) {
        Iterator i$ = shardedJedis.getAllShards().iterator();

        while(i$.hasNext()) {
            Jedis jedis = (Jedis)i$.next();
            if (jedis.getDB() != (long)db) {
                log.debug("to select db::::{}, redis hc:{}", db, shardedJedis.hashCode());
                jedis.select(db);
            }
        }

    }

    private JedisPoolConfig getPoolConfig(Properties props) {
        JedisPoolConfig config = new JedisPoolConfig();
        if (props.getProperty("redis.maxIdle") != null) {
            config.setMaxIdle(Integer.parseInt(props.getProperty("redis.maxIdle")));
        }

        if (props.getProperty("redis.minIdle") != null) {
            config.setMinIdle(Integer.parseInt(props.getProperty("redis.minIdle")));
        }

        Method method;
        if (props.getProperty("redis.maxActive") != null) {
            method = ReflectionUtils.findMethod(JedisPoolConfig.class, "setMaxActive", new Class[]{Integer.TYPE});
            if (method == null) {
                method = ReflectionUtils.findMethod(JedisPoolConfig.class, "setMaxTotal", new Class[]{Integer.TYPE});
            }

            if (method != null) {
                ReflectionUtils.invokeMethod(method, config, new Object[]{Integer.parseInt(props.getProperty("redis.maxActive"))});
            }
        }

        if (props.getProperty("redis.maxWait") != null) {
            method = ReflectionUtils.findMethod(JedisPoolConfig.class, "setMaxWait", new Class[]{Integer.TYPE});
            if (method != null) {
                ReflectionUtils.invokeMethod(method, config, new Object[]{Integer.parseInt(props.getProperty("redis.maxWait"))});
            }
        }

        if (props.getProperty("redis.testWhileIdle") != null) {
            config.setTestWhileIdle(Boolean.parseBoolean(props.getProperty("redis.testWhileIdle")));
        }

        if (props.getProperty("redis.testOnBorrow") != null) {
            config.setTestOnBorrow(Boolean.parseBoolean(props.getProperty("redis.testOnBorrow")));
        }

        if (props.getProperty("redis.testOnReturn") != null) {
            config.setTestOnReturn(Boolean.parseBoolean(props.getProperty("redis.testOnReturn")));
        }

        if (props.getProperty("redis.whenExhaustedAction") != null) {
            config.setBlockWhenExhausted(false);
        }

        if (props.containsKey("redis.timeBetweenEvictionRunsMillis")) {
            config.setTimeBetweenEvictionRunsMillis((long)Byte.parseByte(props.getProperty("redis.timeBetweenEvictionRunsMillis")));
        }

        if (props.containsKey("redis.minEvictableIdleTimeMillis")) {
            config.setMinEvictableIdleTimeMillis((long)Byte.parseByte(props.getProperty("redis.minEvictableIdleTimeMillis")));
        }

        return config;
    }

    @Override
    public <T> T get(String s, Class<T> aClass) {
        return null;
    }

    @Override
    public <T> T get(String s, String s1, Class<T> aClass) {
        return null;
    }

    @Override
    public Object get(String s) {
        return null;
    }

    @Override
    public byte[] get(byte[] bytes) {
        return new byte[0];
    }

    @Override
    public Object get(String s, String s1) {
        return null;
    }

    @Override
    public Object getRawData(String s) {
        return null;
    }

    @Override
    public Object getRawData(String s, String s1) {
        return null;
    }

    @Override
    public void put(String s, Object o, int i, int i1) {

    }

    public void put(String dir, String key, Object object, int expireTime, int maxSizePerObj) {
        {
            if (object != null) {
                long beginSerialize = System.currentTimeMillis();
                Object value = AbstractCache.toStoreValue(object, maxSizePerObj);
                long endSerialize = System.currentTimeMillis();
                if (value != null) {
                    long beginTime = System.currentTimeMillis();
                    String wpkey = AbstractCache.genCacheKey(dir, key);
                    boolean var25 = false;

                    label77: {
                        long costTime;
                        long serializeCostTime;
                        label76: {
                            try {
                                var25 = true;
                                this.setValue(wpkey, value, expireTime);
                                var25 = false;
                                break label76;
                            } catch (Exception var26) {
                                log.error("{}", var26);
                                var26.printStackTrace();
                                var25 = false;
                            } finally {
                                if (var25) {
                                     costTime = System.currentTimeMillis() - beginTime;
                                    if (costTime >= 200L) {
                                        serializeCostTime = endSerialize - beginSerialize;
                                        log.warn("存储缓存数据对象耗费{}毫秒，其中序列化耗费{}毫秒，超过{}ms,key=" + wpkey + ",value =" + value, new Object[]{costTime, serializeCostTime, 200});
                                    }

                                }
                            }

                            costTime = System.currentTimeMillis() - beginTime;
                            if (costTime >= 200L) {
                                serializeCostTime = endSerialize - beginSerialize;
                                log.warn("存储缓存数据对象耗费{}毫秒，其中序列化耗费{}毫秒，超过{}ms,key=" + wpkey + ",value =" + value, new Object[]{costTime, serializeCostTime, 200});
                            }
                            break label77;
                        }

                        costTime = System.currentTimeMillis() - beginTime;
                        if (costTime >= 200L) {
                            serializeCostTime = endSerialize - beginSerialize;
                            log.warn("存储缓存数据对象耗费{}毫秒，其中序列化耗费{}毫秒，超过{}ms,key=" + wpkey + ",value =" + value, new Object[]{costTime, serializeCostTime, 200});
                        }
                    }

                    value = null;
                }
            }
        }
    }

    @Override
    public void put(String key, Object object) {
        this.put(key, object, -1, -1);
    }

    @Override
    public void put(String dir, String key, Object object) {
        this.put(dir, key, object, -1, -1);
    }


    @Override
    public void put(String s, Object o, int i) {

    }

    @Override
    public void put(String s, String s1, Object o, int i) {

    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public boolean isSharding() {
        return this.shardedJedisPool != null;
    }

    public ProxyJedis getRedisClient() {
        ProxyJedis client = null;

        try {
            if (this.redisUrl != null) {
                this.jedisPool = this.getJedisPool(this.redisPoolConfig, this.redisUrl, 10);
                if (this.jedisPool != null) {
                    client = this.jedisPool.getResource();
                }
                return client;
            } else {
                log.error("Config data  'redis.url' is null,please check it !!");
                throw new CacheException("Config data  'redis.url' is null,please check it !!");
            }
        } catch (Exception var3) {
            log.error("Can't get a redis client instance!!!", var3);
            if (client != null) {
               client.close();
            }
            return null;
        }
    }

    public void returnResource(ProxyJedis redis) {
        redis.close();
    }

    public void returnBrokenResource(ProxyJedis redis) {
        redis.close();
    }

    protected Object getValue(Object key) {
        return key instanceof byte[] ? this.invokeMethod("get_bytes", key) : this.invokeMethod("get_String", key);
    }

    public int getExpireTime(String dir, String key) {
        Long value = (Long)this.invokeMethod("ttl_String", AbstractCache.genCacheKey(dir, key));
        return value.intValue();
    }

    public int getExpireTime(String key) {
        return this.getExpireTime((String)null, key);
    }

    protected void setValue(String key, Object value, int expireTime) {
        if (value instanceof byte[]) {
            if (expireTime > 0) {
                this.invokeMethod("set_bytes_int_bytes", key.getBytes(), expireTime, value);
            } else {
                this.invokeMethod("set_bytes_bytes", key.getBytes(), value);
            }
        } else if (expireTime > 0) {
            this.invokeMethod("set_String_int_String", key, expireTime, value.toString());
        } else {
            this.invokeMethod("set_String_String", key, value.toString());
        }

    }

    public void setExpireTime(String dir, String key, int expireTime) {
        this.invokeMethod("expire_String_int", AbstractCache.genCacheKey(dir, key), expireTime);
    }

    public void setExpireTime(String key, int expireTime) {
        this.setExpireTime((String)null, key, expireTime);
    }

    public long decr(String dir, String key, long value) {
        Double dValue = (Double)this.invokeMethod("zincrby_String_double_String", dir, value * -1L, key);
        return dValue.longValue();
    }

    public long decr(String key, long value) {
        return this.decr("/DEFAULT_COUNT/", key, value);
    }

    public long decr(String dir, String key) {
        return this.decr(dir, key, 1L);
    }

    public long decr(String key) {
        return this.decr(key, 1L);
    }

    public long incr(String dir, String key, long value) {
        Double dValue = (Double)this.invokeMethod("zincrby_String_double_String", dir, value, key);
        return dValue.longValue();
    }

    public long incr(String key, long value) {
        return this.incr("/DEFAULT_COUNT/", key, value);
    }

    public long incr(String dir, String key) {
        return this.incr(dir, key, 1L);
    }

    public long incr(String key) {
        return this.incr(key, 1L);
    }

    public boolean remove(String dir, String key) {
        return this.del(AbstractCache.genCacheKey(dir, key)) > 0L;
    }

    public boolean remove(String key) {
        return this.remove((String)null, key);
    }

    private Long del(String _key) {
        return this.shardedJedisPool != null ? (Long)this.invokeMethod("del_String", _key) : (Long)this.invokeMethod("del_NString", _key);
    }

    public int getMaxKeyLen() {
        return -1;
    }

    protected List<String> reGetListKeys(String dir, String keyWord) {
        List<String> list = new LinkedList();
        String keyPartern = this.genKeyPattern(dir, keyWord);
        String[] servers = this.getServers();
        String[] arr$ = servers;
        int len$ = servers.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            String server = arr$[i$];
            Jedis jedis = new Jedis(server);

            try {
                Set<String> keySet = jedis.keys(keyPartern);
                if (keySet != null && !keySet.isEmpty()) {
                    list.addAll(keySet);
                }
            } finally {
                jedis.disconnect();
                jedis = null;
            }
        }

        return list;
    }

    private String genKeyPattern(String dir, String keyWord) {
        String keyPartern = null;
        if (keyWord != null && !keyWord.trim().equals("")) {
            String _keyWord = keyWord.replace('[', '*').replace(']', '*');
            keyPartern = dir + "*" + _keyWord + "*";
        } else {
            keyPartern = dir + "*";
        }

        return keyPartern;
    }

    public List<String> listKeys(String dir, List<String> excludeSubDirs, String keyWord, boolean refresh, int offset, int limit) {
        if (dir != null && !dir.equals("") && !dir.endsWith("/")) {
            dir = dir + "/";
        }
        String keyPartern = this.genKeyPattern(dir, keyWord);
        List<String> list = this.listKeys(dir, keyPartern, excludeSubDirs, offset, limit);
        return list;
    }

    @Override
    public List<String> listKeys(String s, List<String> list, String s1, int i, int i1) {
        return null;
    }

    private List<String> listKeys(String dir, String keyPattern, List<String> excludeSubDirs, int offset, int limit) {
        return this.scanKeys(dir, keyPattern, excludeSubDirs, offset, limit);
    }

    private List<String> scanKeys(String dir, String keyPattern, List<String> excludeSubDirs, int offset, int limit) {
        int cursor = 0;
        int currIndex = 0;
        String currServer = null;
        String keyOfScanKeys = this.genKeyOfScanKeys(keyPattern, 0, limit);
        String keyOfCursor = "SCAN_NEXT_CURSOR_" + keyOfScanKeys;
        String keyOfCurrServer = "SCAN_CurrServer_" + keyOfScanKeys;
        String keyOfCurrIndex = "SCAN_CURR_INDEX_" + keyOfScanKeys;
        if (offset != 0) {
            currServer = (String)this.get(keyOfCurrServer);
            Object objCur = this.get(keyOfCursor);
            if (objCur != null) {
                cursor = Integer.parseInt(objCur.toString());
            }

            String sIndex = (String)this.get(keyOfCurrIndex);
            currIndex = sIndex != null ? Integer.parseInt(sIndex) : 0;
        }

        String[] _servers = this.getServers();
        String[] servers = new String[_servers.length];
        System.arraycopy(_servers, 0, servers, 0, _servers.length);
        Arrays.sort(servers);
        int i = 0;
        if (currServer != null) {
            if (cursor == 0 && offset != 0 && currIndex == 0 && currServer.equals("END")) {
                return null;
            }

            while(i < servers.length && !servers[i].equals(currServer)) {
                ++i;
            }
        }

        try {
            Method scanMethod = ReflectionUtils.findMethod(Jedis.class, "scan", new Class[]{Integer.TYPE, Class.forName("redis.clients.jedis.ScanParams")});
            Object scanParamsObj = this.buildScanParamsObj(keyPattern, 10000);
            int maxLen = limit;
            List<String> resList = new ArrayList(limit);
            boolean firstScan = true;

            for(Jedis jedis = null; i < servers.length; ++i) {
                jedis = new Jedis(servers[i]);

                try {
                    while(resList.size() < maxLen) {
                        Object res = ReflectionUtils.invokeMethod(scanMethod, jedis, new Object[]{cursor, scanParamsObj});
                        int lastCursor = cursor;
                        cursor = this.getCursorFromScanResult(res);
                        List<String> keyList = this.getListFromScanResult(res);
                        log.debug("scan len:" + keyList.size());
                        keyList = this.clearKeyInSubDirs(dir, keyList, excludeSubDirs);
                        if (firstScan && currIndex > 0) {
                            keyList = keyList.subList(currIndex, keyList.size());
                            firstScan = false;
                        }

                        int needKeyCount = maxLen - resList.size();
                        boolean hasSurplus = false;
                        if (needKeyCount >= keyList.size()) {
                            resList.addAll(keyList);
                        } else {
                            resList.addAll(keyList.subList(0, needKeyCount));
                            cursor = lastCursor;
                            hasSurplus = true;
                        }

                        if (resList.size() == maxLen) {
                            this.put(keyOfCursor, cursor);
                            this.put(keyOfCurrServer, servers[i]);
                            if (cursor == 0 && i + 1 < servers.length && !hasSurplus) {
                                this.put(keyOfCurrServer, servers[i + 1]);
                            }

                            if (needKeyCount < keyList.size()) {
                                this.put(keyOfCurrIndex, needKeyCount);
                            } else {
                                this.put(keyOfCurrIndex, 0);
                            }

                            ArrayList var27 = (ArrayList) resList;
                            return var27;
                        }

                        if (cursor == 0) {
                            break;
                        }
                    }
                } finally {
                    if (jedis != null) {
                        jedis.disconnect();
                        jedis = null;
                    }

                }
            }

            this.put(keyOfCursor, cursor);
            this.put(keyOfCurrServer, "END");
            this.put(keyOfCurrIndex, 0);
            return resList;
        } catch (Exception var32) {
            log.error("Scan keys error!", var32);
            return null;
        }
    }

    protected List<String> clearKeyInSubDirs(String dir, List<String> keys, List<String> excludeSubDirs) {
        List<String> keyList = new LinkedList();
        if (excludeSubDirs != null && excludeSubDirs.size() != 0) {
            Iterator i$ = keys.iterator();

            while(i$.hasNext()) {
                String key = (String)i$.next();
                String keyNoFirstDir = key.substring(dir.length());
                if (!this.isKeyInSubDir(keyNoFirstDir, excludeSubDirs)) {
                    keyList.add(keyNoFirstDir);
                }
            }

            return keyList;
        } else {
            return keys;
        }
    }

    protected boolean isKeyInSubDir(String key, List<String> excludeSubDirs) {
        if (excludeSubDirs == null) {
            return false;
        } else {
            Iterator i$ = excludeSubDirs.iterator();

            String dir;
            do {
                if (!i$.hasNext()) {
                    return false;
                }

                dir = (String)i$.next();
            } while(!key.startsWith(dir.endsWith("/") ? dir : dir + "/"));

            return true;
        }
    }

    private int getCursorFromScanResult(Object res) {
        Field field = ReflectionUtils.findField(res.getClass(), "cursor");
        ReflectionUtils.makeAccessible(field);
        Object cursor = ReflectionUtils.getField(field, res);
        return cursor != null ? Integer.parseInt(cursor.toString()) : 0;
    }

    private List<String> getListFromScanResult(Object res) {
        Field field = ReflectionUtils.findField(res.getClass(), "results");
        ReflectionUtils.makeAccessible(field);
        Object results = ReflectionUtils.getField(field, res);
        return results != null ? (List)results : null;
    }

    private Object buildScanParamsObj(String pattern, int count) {
        try {
            Class<?> clz = Class.forName("redis.clients.jedis.ScanParams");
            Object obj = clz.newInstance();
            Method matchMethod = ReflectionUtils.findMethod(clz, "match", new Class[]{String.class});
            ReflectionUtils.invokeMethod(matchMethod, obj, new Object[]{pattern});
            Method countMethod = ReflectionUtils.findMethod(clz, "count", new Class[]{Integer.TYPE});
            ReflectionUtils.invokeMethod(countMethod, obj, new Object[]{count});
            return obj;
        } catch (Exception var7) {
            log.error("Can't find class {}", "redis.clients.jedis.ScanParams", var7);
            throw new CacheException("Can't find class redis.clients.jedis.ScanParams");
        }
    }

    private String genKeyOfScanKeys(String keyPattern, int offset, int limit) {
        StringBuffer keyOfScanKeys = new StringBuffer();
        keyOfScanKeys.append("SCAN_KEYS");
        keyOfScanKeys.append("_");
        keyOfScanKeys.append(keyPattern);
        keyOfScanKeys.append("_");
        keyOfScanKeys.append(offset / 10);
        keyOfScanKeys.append("_");
        keyOfScanKeys.append(limit);
        return keyOfScanKeys.toString();
    }

    private boolean existsScanMethod() {
        boolean findScan = false;

        try {
            findScan = ReflectionUtils.findMethod(Jedis.class, "scan", new Class[]{Integer.TYPE, Class.forName("redis.clients.jedis.ScanParams")}) != null;
        } catch (ClassNotFoundException var3) {
            ;
        }

        if (findScan) {
            log.debug("Find scan method!");
        }

        return findScan;
    }

//    public String[] getServers() {
//        this.redisUrl = this.getCacheServerUrl("redis.url");
//        return this.redisUrl.split(",");
//    }

    public long getKeyCount(String dir, String key) {
        Double cnt = (Double)this.invokeMethod("zscore_String_String", dir, key);
        return cnt == null ? -1L : cnt.longValue();
    }

    public long getKeyCount(String key) {
        return this.getKeyCount("/DEFAULT_COUNT/", key);
    }

    public List<KeyCount> listTopCount(String dir, int sortFlag, int offset, int amount) {
        List<KeyCount> list = new LinkedList();
        Set set = sortFlag == 0 ? (Set)this.invokeMethod("zrangeWithScores_long_long", dir, offset * amount, amount < 0 ? -1 : (offset + 1) * amount - 1) : (Set)this.invokeMethod("zrevrangeWithScores_long_long", dir, offset * amount, amount < 0 ? -1 : (offset + 1) * amount - 1);
        Iterator i$ = set.iterator();

        while(i$.hasNext()) {
            Object obj = i$.next();
            Tuple one = (Tuple)obj;
            KeyCount kc = new KeyCount();
            kc.setKey(one.getElement());
            Double cnt = one.getScore();
            kc.setCount(cnt.longValue());
            list.add(kc);
        }

        return list;
    }

    public List<KeyCount> listTopCount(String dir, int sortFlag, boolean refresh, int offset, int amount) {
        return this.listTopCount(dir, sortFlag, offset, amount);
    }

    public void removeKeyCount(String dir, String key) {
        this.invokeMethod("zrem_String_NString", dir, key);
    }

    public void removeKeyCount(String key) {
        this.removeKeyCount("/DEFAULT_COUNT/", key);
    }

    private Object invokeMethod(String mtdName, Object... args) {
        String initTimeSwith = this.props.getProperty("redis.connWhenStart");
        if ("N".equals(initTimeSwith)) {
            if (this.redisUrl == null) {
                log.error("Config data  'redis.url' is null,please check it !!");
                throw new CacheException("Config data  'redis.url' is null,please check it !!");
            }

            String[] urls = this.redisUrl.split(",");
            if (urls.length > 1) {
                this.shardedJedisPool = this.getShardedJedisPool(this.redisPoolConfig, this.redisUrl, 10);
            } else {
                this.jedisPool = this.getJedisPool(this.redisPoolConfig, this.redisUrl, 10);
            }
        }

        Method method = (Method)JEDIS_METHOD.get(mtdName);
        if (method == null) {
            log.error("Can't not find Mehtod by [" + mtdName + "]!!!");
            return null;
        } else {
            ProxyJedis client = this.getRedisClient();
            if (client == null) {
                return null;
            } else {
                boolean isBroken = false;

                try {
                    Object var7 = method.invoke(client, Utils.getParamArray(method, args));
                    return var7;
                } catch (IllegalArgumentException var13) {
                    log.error("Invoke method[" + mtdName + "] error!!!", var13);
                } catch (IllegalAccessException var14) {
                    log.error("Invoke method[" + mtdName + "] error!!!", var14);
                } catch (InvocationTargetException var15) {
                    log.error("Invoke method[" + mtdName + "] error!!!", var15.getTargetException());
                    if (var15.getTargetException() instanceof JedisConnectionException) {
                        isBroken = true;
                        this.returnBrokenResource(client);
                    }
                } finally {
                    if (!isBroken) {
                        this.returnResource(client);
                    }

                }

                return null;
            }
        }
    }

    public synchronized void destroy() {
        if (!this.state.equals(State.STOPING) && !this.state.equals(State.STOPPED)) {
            this.state = State.STOPING;
            CacheFactory.getStatusMonitorTreadPools().shutdown();
            if (this.shardedJedisPool != null) {
                this.shardedJedisPool.destroy();
            } else {
                this.jedisPool.close();
            }

            this.state = State.STOPPED;
        }
    }

    @Override
    public boolean isAlive(String s) {
        return false;
    }

    @Override
    public boolean isAlive(String s, String s1) {
        return false;
    }

    protected String getServerPortByKey(String key) {
        if (this.jedisPool != null) {
            return this.getServerPort(this.getServers()[0]);
        } else {
            if (this.shardedJedis == null) {
                ShardedJedisPool pool = this.getShardedJedisPool(this.redisPoolConfig, this.redisUrl, 10);
                if (pool == null) {
                    return null;
                }

                this.shardedJedis = pool.getResource();
            }

            JedisShardInfo shardInfo = (JedisShardInfo)this.shardedJedis.getShardInfo(key);
            return shardInfo.getHost() + ":" + shardInfo.getPort();
        }
    }

//    private void startConnStatusMgrThread() {
//        ScheduledExecutorService scheduled = CacheFactory.getStatusMonitorTreadPools();
//        CtgJedisCache.RedisConnStatusMgrThread run1 = new CtgJedisCache.RedisConnStatusMgrThread(this, this.getServers());
//        scheduled.scheduleWithFixedDelay(run1, 5L, 3L, TimeUnit.SECONDS);
//    }

    private void buildMapServer2ServerPort() {
        this.mapServerToServerPort = new HashMap();
        String[] servers = this.getServers();
        String[] arr$ = servers;
        int len$ = servers.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            String server = arr$[i$];
            URI uri = URI.create(server);
            this.mapServerToServerPort.put(server, uri.getHost() + ":" + uri.getPort());
        }

    }

    private void buildRedisDbInfo(int timeout) {
        this.redisDBInfos = new HashMap();
        String[] servers = this.getServers();
        String[] arr$ = servers;
        int len$ = servers.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            String server = arr$[i$];
            RedisDBInfo dbInfo = Utils.getRedisDBInfoFromURI(server, timeout);
            this.redisDBInfos.put(server, dbInfo);
        }

    }

    protected String getServerPort(String server) {
        return (String)this.mapServerToServerPort.get(server);
    }

    public Long expire(byte[] key, int seconds) {
        return (Long)this.invokeMethod("expire", key, seconds);
    }

    public long ttl(String key) {
        return (Long)this.invokeMethod("ttl", key);
    }

    public long pttl(String key) {
        return (Long)this.invokeMethod("pttl", key.getBytes());
    }

    public boolean exists(String key) {
        return (Boolean)this.invokeMethod("exists", key);
    }

    public Long hset(String hash, String key, String value) {
        return this.hset(hash.getBytes(), key.getBytes(), value.getBytes());
    }

    public Long hset(byte[] hash, byte[] key, byte[] value) {
        return (Long)this.invokeMethod("hset_byte", hash, key, value);
    }

    public Long hsetnx(String hash, String key, String value) {
        return (Long)this.invokeMethod("hsetnx", hash, key, value);
    }

    public Long hsetPojo(String hash, String key, Object pojo) throws IOException {
        String json = Utils.toCacheJson(pojo);
        return this.hset(hash, key, json);
    }

    public Object hgetPojo(String hash, String key) {
        String json = this.hget(hash, key);

        try {
            return Utils.getRealObject(json);
        } catch (IOException var5) {
            var5.printStackTrace();
            return null;
        } catch (ClassNotFoundException var6) {
            log.error("jvm中没有找到要实例化的类定义:{}", json);
            var6.printStackTrace();
            return null;
        }
    }

    /** @deprecated */
    @Deprecated
    public Long hset(String hash, String key, String value, int seconds) {
        Long flag = this.hlen(hash);
        Long res = this.hset(hash, key, value);
        if (flag == 0L) {
            this.expire(key.getBytes(), seconds);
        }

        return res;
    }

    /** @deprecated */
    @Deprecated
    public Long hset(byte[] hash, byte[] key, byte[] value, int seconds) {
        Long flag = this.hlen(hash);
        Long res = this.hset(hash, key, value);
        if (flag == 0L) {
            this.expire(key, seconds);
        }

        return res;
    }

    public String hget(String hash, String key) {
        byte[] tmp = this.hget(hash.getBytes(), key.getBytes());
        return tmp != null ? new String(tmp) : "";
    }

    public byte[] hget(byte[] hash, byte[] key) {
        return (byte[])((byte[])this.invokeMethod("hget_byte", hash, key));
    }

    public Boolean hexists(byte[] hash, byte[] key) {
        return (Boolean)this.invokeMethod("hexists_byte", hash, key);
    }

    public Boolean hexists(String hash, String key) {
        return this.hexists(hash.getBytes(), key.getBytes());
    }

    public Long hincrby(byte[] hash, byte[] key, Long value) {
        return (Long)this.invokeMethod("hincrby_byte_long", hash, key, value);
    }

    public Long hincrby(String hash, String key, Long value) {
        return this.hincrby(hash.getBytes(), key.getBytes(), value);
    }

    public Long hlen(byte[] hash) {
        return (Long)this.invokeMethod("hlen_byte", hash);
    }

    public Long hlen(String hash) {
        return this.hlen(hash.getBytes());
    }

    public Long hdel(byte[] hash, byte[] key) {
        return (Long)this.invokeMethod("hdel_byte", hash, new byte[][]{key});
    }

    public Long hdel(String hash, String key) {
        return this.hdel(hash.getBytes(), key.getBytes());
    }

    public Set<String> hkeys(byte[] hash) {
        Set<byte[]> set = (Set)this.invokeMethod("hkeys_byte", hash);
        Set<String> ss = new HashSet();
        Iterator iterator = set.iterator();

        while(iterator.hasNext()) {
            byte[] bs = (byte[])((byte[])iterator.next());
            ss.add(new String(bs));
        }

        return ss;
    }

    public Set<String> hkeys(String hash) {
        return this.hkeys(hash.getBytes());
    }

    public List<byte[]> hvals(byte[] hash) {
        return (List)this.invokeMethod("hvals_byte", hash);
    }

    public List<byte[]> hvals(String hash) {
        return this.hvals(hash.getBytes());
    }

    public Map<byte[], byte[]> hgetall(byte[] hash) {
        return (Map)this.invokeMethod("hgetall_byte", hash);
    }

    public Map<String, byte[]> hgetall(String hash) {
        Map<byte[], byte[]> map = this.hgetall(hash.getBytes());
        Map<String, byte[]> res = new HashMap();
        Set<byte[]> set = map.keySet();
        Iterator i$ = set.iterator();

        while(i$.hasNext()) {
            byte[] bs = (byte[])i$.next();
            res.put(new String(bs), map.get(bs));
        }

        return res;
    }

    public Map<String, Object> hgetallPojo(String hash) {
        Map<byte[], byte[]> map = this.hgetall(hash.getBytes());
        Map<String, Object> res = new HashMap();
        Set<byte[]> set = map.keySet();
        Iterator i$ = set.iterator();

        while(i$.hasNext()) {
            byte[] bs = (byte[])i$.next();
            String json = new String((byte[])map.get(bs));

            try {
                res.put(new String(bs), Utils.getRealObject(json));
            } catch (IOException var9) {
                var9.printStackTrace();
            } catch (ClassNotFoundException var10) {
                log.error("jvm中没有找到要实例化的类定义:{}", json);
                var10.printStackTrace();
            }
        }

        return res;
    }

    public void sadd(String key, String member) {
        this.invokeMethod("sadd_String_NString", key, member);
    }

    public void sadd(String key, List<String> member) {
    }

    public void srem(String key, String member) {
        this.invokeMethod("srem_String_NString", key, member);
    }

    public Collection getMembers(String key) {
        return (Collection)this.invokeMethod("smembers_String", key);
    }

    public void batchRemove(String dir, List<String> keyList) {}

    @Override
    public String[] getServers() {
        return null;
    }

    public void removeAll() {}

    public String getCacheBackend() {
        return "redis";
    }

    @Override
    public ICache getCurrentCache() {
        return null;
    }

    public JedisPoolConfig getRedisPoolConfig() {
        return this.redisPoolConfig;
    }

    public void setRedisPoolConfig(JedisPoolConfig redisPoolConfig) {
        this.redisPoolConfig = redisPoolConfig;
    }

    public Long lpush(String key, Object... values) {
        Object[] args = this.toObjectArray(values);
        return (Long)this.invokeMethod("lpush_NString", key, args);
    }

    public Long rpush(String key, Object... values) {
        Object[] args = this.toObjectArray(values);
        return (Long)this.invokeMethod("rpush_NString", key, args);
    }

    private Object[] toObjectArray(Object[] values) {
        Object[] args = new String[values.length];

        for(int i = 0; i < values.length; ++i) {
            try {
                Object value = null;
                if (Utils.isBasicType(values[i].getClass())) {
                    value = values[i].toString();
                } else {
                    value = (String) AbstractCache.buildCacheObject(values[i]);
                }

                args[i] = (String)value;
            } catch (IOException var5) {
                throw new CacheException(var5);
            }
        }

        return args;
    }

    public Object blpop(int timout, String key) {
        List<String> list = (List)this.invokeMethod("blpop_int_NString", timout, key);
        if (list != null && list.get(1) != null) {
            String value = (String)list.get(1);

            try {
                return value instanceof String && Utils.isPossibleJSON(value) ? Utils.getObjFromJson(value) : value;
            } catch (IOException var6) {
                throw new CacheException(var6);
            } catch (ClassNotFoundException var7) {
                throw new CacheException(var7);
            }
        } else {
            return null;
        }
    }

    public List<Object> blpop(int timeout, String key, int maxElements) {
        if (this.shardedJedisPool != null) {
            throw new CacheException("Can't use this method in shard redis!!!");
        } else {
            Object firstObj = this.blpop(timeout, key);
            if (firstObj == null) {
                return null;
            } else {
                List<Object> returnList = new LinkedList();
                returnList.add(firstObj);
                if (maxElements < 2) {
                    return returnList;
                } else {
                    ProxyJedis jedis = null;
                    boolean isBroken = false;

                    try {
                        jedis = (ProxyJedis)this.getRedisClient();
                        Pipeline pl = jedis.pipelined();
                        List<Response<String>> respList = new ArrayList(maxElements - 1);

                        Response resp;
                        for(int i = 0; i < maxElements - 1; ++i) {
                            resp = pl.lpop(key);
                            respList.add(resp);
                        }

                        pl.sync();
                        Iterator i$ = respList.iterator();

                        while(i$.hasNext()) {
                            resp = (Response)i$.next();
                            if (resp.get() != null) {
                                if (Utils.isPossibleJSON((String)resp.get())) {
                                    returnList.add(Utils.getObjFromJson((String)resp.get()));
                                } else {
                                    returnList.add(resp.get());
                                }
                            }
                        }
                    } catch (Exception var15) {
                        isBroken = true;
                    } finally {
                        if (jedis != null) {
                            if (isBroken) {
                                this.returnBrokenResource(jedis);
                            } else {
                                this.returnResource(jedis);
                            }
                        }

                    }

                    return returnList;
                }
            }
        }
    }

    public Object lpop(String key) {
        String value = (String)this.invokeMethod("lpop_String", key);

        try {
            return value instanceof String && Utils.isPossibleJSON(value) ? Utils.getObjFromJson(value) : value;
        } catch (IOException var4) {
            throw new CacheException(var4);
        } catch (ClassNotFoundException var5) {
            throw new CacheException(var5);
        }
    }

    public long setnx(String key, String value) {
        return (Long)this.invokeMethod("setnx_String_String", key, value);
    }

    public boolean checkAlive() {
        ProxyJedis resource = this.getRedisClient();
        if (resource == null) {
            log.error("Can't not get resource from jedis pool!!!");
            return false;
        } else {
            boolean isAlive = false;

            try {
                ProxyJedis jedis = (ProxyJedis)resource;
                if ("PONG".equalsIgnoreCase(jedis.ping())) {
                    isAlive = true;
                }
                return isAlive;
            } catch (Exception var10) {
                log.error("Check alive fail!!!", var10);
                this.returnBrokenResource(resource);
                return isAlive;
            } finally {
                this.returnResource(resource);
            }
        }
    }

    public boolean putIfNotExists(String key, Object object) {
        if (object == null) {
            return false;
        } else {
            Object value = AbstractCache.toStoreValue(object, -1);
            if (value == null) {
                return false;
            } else {
                return this.setnx(key, value.toString()) == 1L;
            }
        }
    }

    public Long lpush(byte[] key, byte[]... value) {
        return (Long)this.invokeMethod("REDIS_LPUSH_BYTE", key, value);
    }

    public Long lpush(String key, String... value) {
        return (Long)this.invokeMethod("REDIS_LPUSH", key, value);
    }

    public Long rpush(String key, String... value) {
        return (Long)this.invokeMethod("REDIS_RPUSH", key, value);
    }

    public Long rpush(byte[] key, byte[]... value) {
        return (Long)this.invokeMethod("REDIS_RPUSH_BYTE", key, value);
    }

    public Long linsert(String key, BinaryClient.LIST_POSITION position, String pivot, String value) {
        return (Long)this.invokeMethod("REDIS_LINSERT", key, position, pivot, value);
    }

    public Long linsert(byte[] key, BinaryClient.LIST_POSITION position, byte[] pivot, byte[] value) {
        return (Long)this.invokeMethod("REDIS_LINSERT_BYTE", key, position, pivot, value);
    }

    public boolean lset(String key, long index, String value) {
        return "OK".equals(this.invokeMethod("REDIS_LSET", key, index, value));
    }

    public boolean lset(byte[] key, long index, byte[] value) {
        return "OK".equals(this.invokeMethod("REDIS_LSET_BYTE", key, index, value));
    }

    public Long lrem(String key, long count, String value) {
        return (Long)this.invokeMethod("REDIS_LREM", key, count, value);
    }

    public Long lrem(byte[] key, long count, byte[] value) {
        return (Long)this.invokeMethod("REDIS_LREM_BYTE", key, count, value);
    }

    public boolean ltrim(String key, long start, long end) {
        return "OK".equals(this.invokeMethod("REDIS_LTRIM", key, start, end));
    }

    public boolean ltrim(byte[] key, long start, long end) {
        return "OK".equals(this.invokeMethod("REDIS_LTRIM_BYTE", key, start, end));
    }

    public byte[] lpop(byte[] key) {
        return (byte[])((byte[])this.invokeMethod("REDIS_LPOP_BYTE", key));
    }

    public String lpop_ori(String key) {
        return (String)this.invokeMethod("REDIS_LPOP", key);
    }

    public String rpop(String key) {
        return (String)this.invokeMethod("REDIS_RPOP", key);
    }

    public byte[] rpop(byte[] key) {
        return (byte[])((byte[])this.invokeMethod("REDIS_RPOP_BYTE", key));
    }

    public void doNotCluster() {
        if (this.redisUrl.split(",").length > 1) {
            throw new CacheException("Can't use this method in shard redis!!!");
        }
    }

    public List<String> blpop(int timeout, String... keys) {
        this.doNotCluster();
        return (List)this.invokeMethod("REDIS_BLPOP", timeout, keys);
    }

    public List<byte[]> blpop(int timeout, byte[]... keys) {
        this.doNotCluster();
        return (List)this.invokeMethod("REDIS_BLPOP_BYTE", timeout, keys);
    }

    public List<String> brpop(int timeout, String... keys) {
        this.doNotCluster();
        return (List)this.invokeMethod("REDIS_BRPOP", timeout, keys);
    }

    public List<byte[]> brpop(int timeout, byte[]... keys) {
        this.doNotCluster();
        return (List)this.invokeMethod("REDIS_BRPOP_BYTE", timeout, keys);
    }

    public String lindex(String key, long index) {
        return (String)this.invokeMethod("REDIS_LINDEX", key, index);
    }

    public byte[] lindex(byte[] key, long index) {
        return (byte[])((byte[])this.invokeMethod("REDIS_LINDEX_BYTE", key, index));
    }

    public long llen(String key) {
        return (Long)this.invokeMethod("REDIS_LLEN", key);
    }

    public long llen(byte[] key) {
        return (Long)this.invokeMethod("REDIS_LLEN_BYTE", key);
    }

    public List<String> lrange(String key, long start, long end) {
        return (List)this.invokeMethod("REDIS_LRANGE", key, start, end);
    }

    public List<byte[]> lrange(byte[] key, long start, long end) {
        return (List)this.invokeMethod("REDIS_LRANGE_BYTE", key, start, end);
    }

    public Boolean sadd(byte[] key, byte[]... members) {
        return (Long)this.invokeMethod("REDIS_SADD_BYTE", key, members) > 0L;
    }

    public Boolean srem(byte[] key, byte[]... members) {
        return (Long)this.invokeMethod("REDIS_SREM_BYTE", key, members) > 0L;
    }

    public String spop(String key) {
        return (String)this.invokeMethod("REDIS_SPOP", key);
    }

    public byte[] spop(byte[] key) {
        return (byte[])((byte[])this.invokeMethod("REDIS_SPOP_BYTE", key));
    }

    public long scard(String key) {
        return (Long)this.invokeMethod("REDIS_SCARD", key);
    }

    public long scard(byte[] key) {
        return (Long)this.invokeMethod("REDIS_SCARD_BYTE", key);
    }

    public Boolean sismember(String key, String member) {
        return (Boolean)this.invokeMethod("REDIS_SISMEMBER", key, member);
    }

    public Boolean sismember(byte[] key, byte[] member) {
        return (Boolean)this.invokeMethod("REDIS_SISMEMBER_BYTE", key, member);
    }

    public String srandmember(String key) {
        return (String)this.invokeMethod("REDIS_SRANDMEMBER", key);
    }

    public byte[] srandmember(byte[] key) {
        return (byte[])((byte[])this.invokeMethod("REDIS_SRANDMEMBER_BYTE", key));
    }

    public Set<String> smembers(String key) {
        return (Set)this.invokeMethod("REDIS_SMEMBERS", key);
    }

    public Set<byte[]> smembers(byte[] key) {
        return (Set)this.invokeMethod("REDIS_SMEMBERS_BYTE", key);
    }

    public boolean delkey(String key) {
        return this.del(key) > 0L;
    }

    public String type(String key) {
        return (String)this.invokeMethod("type", key);
    }

    public String type(byte[] key) {
        return (String)this.invokeMethod("type_byte", key);
    }

    public String set(String key, String value, String nxxx, String expx, int time) {
        return (String)this.invokeMethod("set", key, value, nxxx, expx, time);
    }

    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        return (String)this.invokeMethod("set_byte", key, value, nxxx, expx, time);
    }

    public String setex(String key, int timeout, String value) {
        return (String)this.invokeMethod("set_String_int_String", key, timeout, value);
    }

    public String setex(byte[] key, int timeout, byte[] value) {
        return (String)this.invokeMethod("set_bytes_int_bytes", key, timeout, value);
    }

    public String set(byte[] key, byte[] value) {
        return (String)this.invokeMethod("set_simple_byte", key, value);
    }

    public String set(String key, String value) {
        return (String)this.invokeMethod("set_simple", key, value);
    }

    public boolean lock(String lockname, String lockvalue, Long timeout, Long expire) {
        return DistLock.lock(this, lockname, lockvalue, timeout, expire);
    }

    public boolean lock(String lockname, String lockvalue, int timeout, int expire) {
        return DistLock.lock(this, lockname, lockvalue, new Long((long)timeout), new Long((long)expire));
    }

    public boolean unlock(String lockname, String lockvalue) {
        return DistLock.unlock(this, lockname, lockvalue);
    }

    public boolean unlock(String lockname) {
        return DistLock.unlock(this, lockname, (String)null);
    }

    public boolean relock(String lockname, String lockvalue, Long timeout, Long expire) {
        return DistLock.relock(this, lockname, lockvalue, timeout, expire);
    }

    public boolean relock(String lockname, String lockvalue, int timeout, int expire) {
        return DistLock.relock(this, lockname, lockvalue, new Long((long)timeout), new Long((long)expire));
    }

    public boolean unrelock(String lockname, String lockvalue) {
        return DistLock.unrelock(this, lockname, lockvalue);
    }

    public boolean unrelock(String lockname) {
        return DistLock.unrelock(this, lockname, (String)null);
    }

    static {
        JEDIS_METHOD.put("get_String", Utils.getDeclaredMethod(JedisCommands.class, "get", new Class[]{String.class}));
        JEDIS_METHOD.put("get_bytes", Utils.getDeclaredMethod(BinaryJedisCommands.class, "get", new Class[]{byte[].class}));
        JEDIS_METHOD.put("set_String_String", Utils.getDeclaredMethod(JedisCommands.class, "set", new Class[]{String.class, String.class}));
        JEDIS_METHOD.put("setnx_String_String", Utils.getDeclaredMethod(JedisCommands.class, "setnx", new Class[]{String.class, String.class}));
        JEDIS_METHOD.put("set_String_int_String", Utils.getDeclaredMethod(JedisCommands.class, "setex", new Class[]{String.class, Integer.TYPE, String.class}));
        JEDIS_METHOD.put("set_bytes_bytes", Utils.getDeclaredMethod(BinaryJedisCommands.class, "set", new Class[]{byte[].class, byte[].class}));
        JEDIS_METHOD.put("set_bytes_int_bytes", Utils.getDeclaredMethod(BinaryJedisCommands.class, "setex", new Class[]{byte[].class, Integer.TYPE, byte[].class}));
        JEDIS_METHOD.put("del_NString", Utils.getDeclaredMethod(Jedis.class, "del", new Class[]{String[].class}));
        JEDIS_METHOD.put("del_String", Utils.getDeclaredMethod(ShardedJedis.class, "del", new Class[]{String.class}));
        JEDIS_METHOD.put("expire_String_int", Utils.getDeclaredMethod(JedisCommands.class, "expire", new Class[]{String.class, Integer.TYPE}));
        JEDIS_METHOD.put("expire_bytes_int", Utils.getDeclaredMethod(BinaryJedisCommands.class, "expire", new Class[]{byte[].class, Integer.TYPE}));
        JEDIS_METHOD.put("ttl_String", Utils.getDeclaredMethod(JedisCommands.class, "ttl", new Class[]{String.class}));
        JEDIS_METHOD.put("zincrby_String_double_String", Utils.getDeclaredMethod(JedisCommands.class, "zincrby", new Class[]{String.class, Double.TYPE, String.class}));
        JEDIS_METHOD.put("zscore_String_String", Utils.getDeclaredMethod(JedisCommands.class, "zscore", new Class[]{String.class, String.class}));
        JEDIS_METHOD.put("zrangeWithScores_long_long", Utils.getDeclaredMethod(JedisCommands.class, "zrangeWithScores", new Class[]{String.class, Long.TYPE, Long.TYPE}));
        JEDIS_METHOD.put("zrevrangeWithScores_long_long", Utils.getDeclaredMethod(JedisCommands.class, "zrevrangeWithScores", new Class[]{String.class, Long.TYPE, Long.TYPE}));
        Method zremMethod = null;
        Method[] arr$ = JedisCommands.class.getMethods();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Method method = arr$[i$];
            if ("zrem".equals(method.getName())) {
                zremMethod = method;
            }
        }

        JEDIS_METHOD.put("zrem_String_NString", zremMethod);
        JEDIS_METHOD.put("sadd_String_NString", Utils.getDeclaredMethod(JedisCommands.class, "sadd", new Class[]{String.class, String[].class}));
        JEDIS_METHOD.put("srem_String_NString", Utils.getDeclaredMethod(JedisCommands.class, "srem", new Class[]{String.class, String[].class}));
        JEDIS_METHOD.put("smembers_String", Utils.getDeclaredMethod(JedisCommands.class, "smembers", new Class[]{String.class}));
        JEDIS_METHOD.put("lpush_NString", Utils.getDeclaredMethod(JedisCommands.class, "lpush", new Class[]{String.class, String[].class}));
        JEDIS_METHOD.put("rpush_NString", Utils.getDeclaredMethod(JedisCommands.class, "rpush", new Class[]{String.class, String[].class}));
        JEDIS_METHOD.put("lpop_String", Utils.getDeclaredMethod(JedisCommands.class, "lpop", new Class[]{String.class}));
        JEDIS_METHOD.put("blpop_int_NString", Utils.getDeclaredMethod(Jedis.class, "blpop", new Class[]{Integer.TYPE, String[].class}));
        JEDIS_METHOD.put("hset_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "hset", new Class[]{byte[].class, byte[].class, byte[].class}));
        JEDIS_METHOD.put("hget_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "hget", new Class[]{byte[].class, byte[].class}));
        JEDIS_METHOD.put("hexists_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "hexists", new Class[]{byte[].class, byte[].class}));
        JEDIS_METHOD.put("hincrby_byte_long", Utils.getDeclaredMethod(BinaryJedisCommands.class, "hincrBy", new Class[]{byte[].class, byte[].class, Long.TYPE}));
        JEDIS_METHOD.put("hlen_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "hlen", new Class[]{byte[].class}));
        JEDIS_METHOD.put("hkeys_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "hkeys", new Class[]{byte[].class}));
        JEDIS_METHOD.put("hvals_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "hvals", new Class[]{byte[].class}));
        JEDIS_METHOD.put("hgetall_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "hgetAll", new Class[]{byte[].class}));
        JEDIS_METHOD.put("hdel_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "hdel", new Class[]{byte[].class, byte[][].class}));
        JEDIS_METHOD.put("hsetnx", Utils.getDeclaredMethod(JedisCommands.class, "hsetnx", new Class[]{String.class, String.class, String.class}));
        JEDIS_METHOD.put("expire", Utils.getDeclaredMethod(BinaryJedisCommands.class, "expire", new Class[]{byte[].class, Integer.TYPE}));
        JEDIS_METHOD.put("ttl", Utils.getDeclaredMethod(JedisCommands.class, "ttl", new Class[]{String.class}));
        JEDIS_METHOD.put("pttl", Utils.getDeclaredMethod(JedisCommands.class, "pttl", new Class[]{byte[].class}));
        JEDIS_METHOD.put("exists", Utils.getDeclaredMethod(JedisCommands.class, "exists", new Class[]{String.class}));
        JEDIS_METHOD.put("set", Utils.getDeclaredMethod(JedisCommands.class, "set", new Class[]{String.class, String.class, String.class, String.class, Long.TYPE}));
        JEDIS_METHOD.put("set_byte", Utils.getDeclaredMethod(JedisCommands.class, "set", new Class[]{String.class, String.class, String.class, String.class, Long.TYPE}));
        JEDIS_METHOD.put("set_simple", Utils.getDeclaredMethod(JedisCommands.class, "set", new Class[]{String.class, String.class}));
        JEDIS_METHOD.put("set_simple_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "set", new Class[]{byte[].class, byte[].class}));
        JEDIS_METHOD.put("REDIS_LPUSH", Utils.getDeclaredMethod(JedisCommands.class, "lpush", new Class[]{String.class, String[].class}));
        JEDIS_METHOD.put("REDIS_LPUSH_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "lpush", new Class[]{byte[].class, byte[][].class}));
        JEDIS_METHOD.put("REDIS_RPUSH", Utils.getDeclaredMethod(JedisCommands.class, "rpush", new Class[]{String.class, String[].class}));
        JEDIS_METHOD.put("REDIS_RPUSH_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "rpush", new Class[]{byte[].class, byte[][].class}));
        JEDIS_METHOD.put("REDIS_LINSERT", Utils.getDeclaredMethod(JedisCommands.class, "linsert", new Class[]{String.class, BinaryClient.LIST_POSITION.class, String.class, String.class}));
        JEDIS_METHOD.put("REDIS_LINSERT_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "linsert", new Class[]{byte[].class, BinaryClient.LIST_POSITION.class, byte[].class, byte[].class}));
        JEDIS_METHOD.put("REDIS_LSET", Utils.getDeclaredMethod(JedisCommands.class, "lset", new Class[]{String.class, Long.TYPE, String.class}));
        JEDIS_METHOD.put("REDIS_LSET_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "lset", new Class[]{byte[].class, Long.TYPE, byte[].class}));
        JEDIS_METHOD.put("REDIS_LREM", Utils.getDeclaredMethod(JedisCommands.class, "lrem", new Class[]{String.class, Long.TYPE, String.class}));
        JEDIS_METHOD.put("REDIS_LREM_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "lrem", new Class[]{byte[].class, Long.TYPE, byte[].class}));
        JEDIS_METHOD.put("REDIS_LTRIM", Utils.getDeclaredMethod(JedisCommands.class, "ltrim", new Class[]{String.class, Long.TYPE, Long.TYPE}));
        JEDIS_METHOD.put("REDIS_LTRIM_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "ltrim", new Class[]{byte[].class, Long.TYPE, Long.TYPE}));
        JEDIS_METHOD.put("REDIS_LPOP", Utils.getDeclaredMethod(JedisCommands.class, "lpop", new Class[]{String.class}));
        JEDIS_METHOD.put("REDIS_LPOP_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "lpop", new Class[]{byte[].class}));
        JEDIS_METHOD.put("REDIS_RPOP", Utils.getDeclaredMethod(JedisCommands.class, "rpop", new Class[]{String.class}));
        JEDIS_METHOD.put("REDIS_RPOP_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "rpop", new Class[]{byte[].class}));
        JEDIS_METHOD.put("REDIS_RPOPLPUSH", Utils.getDeclaredMethod(Jedis.class, "rpoplpush", new Class[]{String.class, String.class}));
        JEDIS_METHOD.put("REDIS_RPOPLPUSH_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "rpoplpush", new Class[]{byte[].class, byte[].class}));
        JEDIS_METHOD.put("REDIS_LINDEX", Utils.getDeclaredMethod(JedisCommands.class, "lindex", new Class[]{String.class, Long.TYPE}));
        JEDIS_METHOD.put("REDIS_LINDEX_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "lindex", new Class[]{byte[].class, Long.TYPE}));
        JEDIS_METHOD.put("REDIS_LLEN", Utils.getDeclaredMethod(JedisCommands.class, "llen", new Class[]{String.class}));
        JEDIS_METHOD.put("REDIS_LLEN_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "llen", new Class[]{byte[].class}));
        JEDIS_METHOD.put("REDIS_BLPOP", Utils.getDeclaredMethod(Jedis.class, "blpop", new Class[]{Integer.TYPE, String[].class}));
        JEDIS_METHOD.put("REDIS_BLPOP_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "blpop", new Class[]{Integer.TYPE, byte[][].class}));
        JEDIS_METHOD.put("REDIS_BRPOP", Utils.getDeclaredMethod(Jedis.class, "brpop", new Class[]{Integer.TYPE, String[].class}));
        JEDIS_METHOD.put("REDIS_BRPOP_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "brpop", new Class[]{Integer.TYPE, byte[][].class}));
        JEDIS_METHOD.put("REDIS_LRANGE", Utils.getDeclaredMethod(JedisCommands.class, "lrange", new Class[]{String.class, Long.TYPE, Long.TYPE}));
        JEDIS_METHOD.put("REDIS_LRANGE_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "lrange", new Class[]{byte[].class, Long.TYPE, Long.TYPE}));
        JEDIS_METHOD.put("REDIS_SADD_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "sadd", new Class[]{byte[].class, byte[][].class}));
        JEDIS_METHOD.put("REDIS_SREM_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "srem", new Class[]{byte[].class, byte[][].class}));
        JEDIS_METHOD.put("REDIS_SPOP", Utils.getDeclaredMethod(JedisCommands.class, "spop", new Class[]{String.class}));
        JEDIS_METHOD.put("REDIS_SPOP_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "spop", new Class[]{byte[].class}));
        JEDIS_METHOD.put("REDIS_SDIFF", Utils.getDeclaredMethod(Jedis.class, "sdiff", new Class[]{String[].class}));
        JEDIS_METHOD.put("REDIS_SDIFF_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "sdiff", new Class[]{byte[][].class}));
        JEDIS_METHOD.put("REDIS_SDIFFSTORE", Utils.getDeclaredMethod(Jedis.class, "sdiffstore", new Class[]{String.class, String[].class}));
        JEDIS_METHOD.put("REDIS_SDIFFSTORE_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "sdiffstore", new Class[]{byte[].class, byte[][].class}));
        JEDIS_METHOD.put("REDIS_SINTER", Utils.getDeclaredMethod(Jedis.class, "sinter", new Class[]{String[].class}));
        JEDIS_METHOD.put("REDIS_SINTER_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "sinter", new Class[]{byte[][].class}));
        JEDIS_METHOD.put("REDIS_SINTERSTORE", Utils.getDeclaredMethod(Jedis.class, "sinterstore", new Class[]{String.class, String[].class}));
        JEDIS_METHOD.put("REDIS_SINTERSTORE_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "sinterstore", new Class[]{byte[].class, byte[][].class}));
        JEDIS_METHOD.put("REDIS_SUNION", Utils.getDeclaredMethod(Jedis.class, "sunion", new Class[]{String[].class}));
        JEDIS_METHOD.put("REDIS_SUNION_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "sunion", new Class[]{byte[][].class}));
        JEDIS_METHOD.put("REDIS_SUNIONSTORE", Utils.getDeclaredMethod(Jedis.class, "sunionstore", new Class[]{String.class, String[].class}));
        JEDIS_METHOD.put("REDIS_SUNIONSTORE_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "sunionstore", new Class[]{byte[].class, byte[][].class}));
        JEDIS_METHOD.put("REDIS_SMOVE", Utils.getDeclaredMethod(Jedis.class, "smove", new Class[]{String.class, String.class, String.class}));
        JEDIS_METHOD.put("REDIS_SMOVE_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "smove", new Class[]{byte[].class, byte[].class, byte[].class}));
        JEDIS_METHOD.put("REDIS_SCARD", Utils.getDeclaredMethod(JedisCommands.class, "scard", new Class[]{String.class}));
        JEDIS_METHOD.put("REDIS_SCARD_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "scard", new Class[]{byte[].class}));
        JEDIS_METHOD.put("REDIS_SISMEMBER", Utils.getDeclaredMethod(JedisCommands.class, "sismember", new Class[]{String.class, String.class}));
        JEDIS_METHOD.put("REDIS_SISMEMBER_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "sismember", new Class[]{byte[].class, byte[].class}));
        JEDIS_METHOD.put("REDIS_SRANDMEMBER", Utils.getDeclaredMethod(JedisCommands.class, "srandmember", new Class[]{String.class}));
        JEDIS_METHOD.put("REDIS_SRANDMEMBER_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "srandmember", new Class[]{byte[].class}));
        JEDIS_METHOD.put("REDIS_SRANDMEMBER_COUNT", Utils.getDeclaredMethod(Jedis.class, "srandmember", new Class[]{String.class, Integer.TYPE}));
        JEDIS_METHOD.put("REDIS_SRANDMEMBER_COUNT_BYTE", Utils.getDeclaredMethod(BinaryJedis.class, "srandmember", new Class[]{byte[].class, Integer.TYPE}));
        JEDIS_METHOD.put("REDIS_SMEMBERS", Utils.getDeclaredMethod(JedisCommands.class, "smembers", new Class[]{String.class}));
        JEDIS_METHOD.put("REDIS_SMEMBERS_BYTE", Utils.getDeclaredMethod(BinaryJedisCommands.class, "smembers", new Class[]{byte[].class}));
        JEDIS_METHOD.put("type", Utils.getDeclaredMethod(JedisCommands.class, "type", new Class[]{String.class}));
        JEDIS_METHOD.put("type_byte", Utils.getDeclaredMethod(BinaryJedisCommands.class, "type", new Class[]{byte[].class}));
    }
}
