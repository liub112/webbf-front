package com.al.crm.redis.proxy;

import com.al.crm.nosql.cache.ICache;
import com.al.crm.nosql.cache.IRedisFix;
import com.al.crm.nosql.cache.KeyCount;
import com.al.crm.redis.ClientRouteSelector;
import redis.clients.jedis.BinaryClient;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultRedisFixProxy implements IRedisFix {


    private ClientRouteSelector<IRedisFix> clientRouteSelector;

    public DefaultRedisFixProxy(ClientRouteSelector<IRedisFix> clientRouteSelector) {
        this.clientRouteSelector = clientRouteSelector;
    }

    @Override
    public boolean delkey(String key) {
        return  clientRouteSelector.determineCurrentClientBean().delkey(key);
    }

    @Override
    public long ttl(String key) {
        return  clientRouteSelector.determineCurrentClientBean().ttl(key);
    }

    @Override
    public long pttl(String key) {
        return  clientRouteSelector.determineCurrentClientBean().pttl(key);
    }

    @Override
    public long setnx(String key, String value) {
        return  clientRouteSelector.determineCurrentClientBean().setnx(key, value);
    }

    @Override
    public Long expire(byte[] key, int seconds) {
        return  clientRouteSelector.determineCurrentClientBean().expire(key, seconds);
    }

    @Override
    public boolean exists(String key) {
        return  clientRouteSelector.determineCurrentClientBean().exists(key);
    }

    @Override
    public Long hset(String hash, String key, String value) {
        return  clientRouteSelector.determineCurrentClientBean().hset(hash, key, value);
    }

    @Override
    public Long hsetnx(String hash, String key, String value) {
        return  clientRouteSelector.determineCurrentClientBean().hsetnx(hash, key, value);
    }

    @Override
    public Long hset(byte[] hash, byte[] key, byte[] value) {
        return  clientRouteSelector.determineCurrentClientBean().hset(hash, key, value);
    }

    @Override
    public Long hsetPojo(String hash, String key, Object pojo) throws IOException {
        return  clientRouteSelector.determineCurrentClientBean().hsetPojo(hash, key, pojo);
    }

    @Override
    public Object hgetPojo(String hash, String key) {
        return  clientRouteSelector.determineCurrentClientBean().hgetPojo(hash, key);
    }

    @Override
    public String hget(String hash, String key) {
        return  clientRouteSelector.determineCurrentClientBean().hget(hash, key);
    }

    @Override
    public byte[] hget(byte[] hash, byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().hget(hash, key);
    }

    @Override
    public Boolean hexists(byte[] hash, byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().hexists(hash, key);
    }

    @Override
    public Boolean hexists(String hash, String key) {
        return  clientRouteSelector.determineCurrentClientBean().hexists(hash, key);
    }

    @Override
    public Long hincrby(byte[] hash, byte[] key, Long value) {
        return  clientRouteSelector.determineCurrentClientBean().hincrby(hash, key, value);
    }

    @Override
    public Long hincrby(String hash, String key, Long value) {
        return  clientRouteSelector.determineCurrentClientBean().hincrby(hash, key, value);
    }

    @Override
    public Long hlen(byte[] hash) {
        return  clientRouteSelector.determineCurrentClientBean().hlen(hash);
    }

    @Override
    public Long hlen(String hash) {
        return  clientRouteSelector.determineCurrentClientBean().hlen(hash);
    }

    @Override
    public Long hdel(byte[] hash, byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().hdel(hash, key);
    }

    @Override
    public Long hdel(String hash, String key) {
        return  clientRouteSelector.determineCurrentClientBean().hdel(hash, key);
    }

    @Override
    public Set<String> hkeys(byte[] hash) {
        return  clientRouteSelector.determineCurrentClientBean().hkeys(hash);
    }

    @Override
    public Set<String> hkeys(String hash) {
        return  clientRouteSelector.determineCurrentClientBean().hkeys(hash);
    }

    @Override
    public List<byte[]> hvals(byte[] hash) {
        return  clientRouteSelector.determineCurrentClientBean().hvals(hash);
    }

    @Override
    public List<byte[]> hvals(String hash) {
        return  clientRouteSelector.determineCurrentClientBean().hvals(hash);
    }

    @Override
    public Map<byte[], byte[]> hgetall(byte[] hash) {
        return  clientRouteSelector.determineCurrentClientBean().hgetall(hash);
    }

    @Override
    public Map<String, byte[]> hgetall(String hash) {
        return  clientRouteSelector.determineCurrentClientBean().hgetall(hash);
    }

    @Override
    public Map<String, Object> hgetallPojo(String hash) {
        return  clientRouteSelector.determineCurrentClientBean().hgetallPojo(hash);
    }

    @Override
    public Long lpush(String key, String... value) {
        return  clientRouteSelector.determineCurrentClientBean().lpush(key, value);
    }

    @Override
    public Long lpush(byte[] key, byte[]... value) {
        return  clientRouteSelector.determineCurrentClientBean().lpush(key, value);
    }

    @Override
    public Long rpush(String key, String... value) {
        return  clientRouteSelector.determineCurrentClientBean().rpush(key, value);
    }

    @Override
    public Long rpush(byte[] key, byte[]... value) {
        return  clientRouteSelector.determineCurrentClientBean().rpush(key, value);
    }

    @Override
    public Long rpush(String key, Object... values) {
        return  clientRouteSelector.determineCurrentClientBean().rpush(key, values);
    }

    @Override
    public Long lpush(String key, Object... values) {
        return  clientRouteSelector.determineCurrentClientBean().lpush(key, values);
    }

    @Override
    public Long linsert(String key, BinaryClient.LIST_POSITION position, String pivot, String value) {
        return  clientRouteSelector.determineCurrentClientBean().linsert(key, position, pivot, value);
    }

    @Override
    public Long linsert(byte[] key, BinaryClient.LIST_POSITION position, byte[] pivot, byte[] value) {
        return  clientRouteSelector.determineCurrentClientBean().linsert(key, position, pivot, value);
    }

    @Override
    public boolean lset(String key, long index, String value) {
        return  clientRouteSelector.determineCurrentClientBean().lset(key, index, value);
    }

    @Override
    public boolean lset(byte[] key, long index, byte[] value) {
        return  clientRouteSelector.determineCurrentClientBean().lset(key, index, value);
    }

    @Override
    public Long lrem(String key, long count, String value) {
        return  clientRouteSelector.determineCurrentClientBean().lrem(key, count, value);
    }

    @Override
    public Long lrem(byte[] key, long count, byte[] value) {
        return  clientRouteSelector.determineCurrentClientBean().lrem(key, count, value);
    }

    @Override
    public boolean ltrim(String key, long start, long end) {
        return  clientRouteSelector.determineCurrentClientBean().ltrim(key, start, end);
    }

    @Override
    public boolean ltrim(byte[] key, long start, long end) {
        return  clientRouteSelector.determineCurrentClientBean().ltrim(key, start, end);
    }

    @Override
    public byte[] lpop(byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().lpop(key);
    }

    @Override
    public String lpop_ori(String key) {
        return  clientRouteSelector.determineCurrentClientBean().lpop_ori(key);
    }

    @Override
    public String rpop(String key) {
        return  clientRouteSelector.determineCurrentClientBean().rpop(key);
    }

    @Override
    public byte[] rpop(byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().rpop(key);
    }

    @Override
    public List<String> blpop(int timeout, String... keys) {
        return  clientRouteSelector.determineCurrentClientBean().blpop(timeout, keys);
    }

    @Override
    public List<byte[]> blpop(int timeout, byte[]... keys) {
        return  clientRouteSelector.determineCurrentClientBean().blpop(timeout, keys);
    }

    @Override
    public List<String> brpop(int timeout, String... keys) {
        return  clientRouteSelector.determineCurrentClientBean().brpop(timeout, keys);
    }

    @Override
    public List<byte[]> brpop(int timeout, byte[]... keys) {
        return  clientRouteSelector.determineCurrentClientBean().brpop(timeout, keys);
    }

    @Override
    public String lindex(String key, long index) {
        return  clientRouteSelector.determineCurrentClientBean().lindex(key, index);
    }

    @Override
    public byte[] lindex(byte[] key, long index) {
        return  clientRouteSelector.determineCurrentClientBean().lindex(key, index);
    }

    @Override
    public long llen(String key) {
        return  clientRouteSelector.determineCurrentClientBean().llen(key);
    }

    @Override
    public long llen(byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().llen(key);
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        return  clientRouteSelector.determineCurrentClientBean().lrange(key, start, end);
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        return  clientRouteSelector.determineCurrentClientBean().lrange(key, start, end);
    }

    @Override
    public Boolean sadd(byte[] key, byte[]... members) {
        return  clientRouteSelector.determineCurrentClientBean().sadd(key, members);
    }

    @Override
    public Boolean srem(byte[] key, byte[]... members) {
        return  clientRouteSelector.determineCurrentClientBean().srem(key, members);
    }

    @Override
    public String spop(String key) {
        return  clientRouteSelector.determineCurrentClientBean().spop(key);
    }

    @Override
    public byte[] spop(byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().spop(key);
    }

    @Override
    public long scard(String key) {
        return  clientRouteSelector.determineCurrentClientBean().scard(key);
    }

    @Override
    public long scard(byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().scard(key);
    }

    @Override
    public Boolean sismember(String key, String member) {
        return  clientRouteSelector.determineCurrentClientBean().sismember(key, member);
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        return  clientRouteSelector.determineCurrentClientBean().sismember(key, member);
    }

    @Override
    public String srandmember(String key) {
        return  clientRouteSelector.determineCurrentClientBean().srandmember(key);
    }

    @Override
    public byte[] srandmember(byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().srandmember(key);
    }

    @Override
    public Set<String> smembers(String key) {
        return  clientRouteSelector.determineCurrentClientBean().smembers(key);
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().smembers(key);
    }

    @Override
    public String type(String key) {
        return  clientRouteSelector.determineCurrentClientBean().type(key);
    }

    @Override
    public String type(byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().type(key);
    }

    @Override
    public String set(String key, String value, String nxxx, String expx, int time) {
        return  clientRouteSelector.determineCurrentClientBean().set(key, value, nxxx, expx, time);
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        return  clientRouteSelector.determineCurrentClientBean().set(key, value, nxxx, expx, time);
    }

    @Override
    public String setex(String key, int timeout, String value) {
        return  clientRouteSelector.determineCurrentClientBean().setex(key, timeout, value);
    }

    @Override
    public String setex(byte[] key, int timeout, byte[] value) {
        return  clientRouteSelector.determineCurrentClientBean().setex(key, timeout, value);
    }

    @Override
    public String set(byte[] key, byte[] value) {
        return  clientRouteSelector.determineCurrentClientBean().set(key, value);
    }

    @Override
    public String set(String key, String value) {
        return  clientRouteSelector.determineCurrentClientBean().set(key, value);
    }

    @Override
    public boolean lock(String lockname, String lockvalue, Long timeout, Long expire) {
        return  clientRouteSelector.determineCurrentClientBean().lock(lockname, lockvalue, timeout, expire);
    }

    @Override
    public boolean lock(String lockname, String lockvalue, int timeout, int expire) {
        return  clientRouteSelector.determineCurrentClientBean().lock(lockname, lockvalue, timeout, expire);
    }

    @Override
    public boolean unlock(String lockname, String lockvalue) {
        return  clientRouteSelector.determineCurrentClientBean().unlock(lockname, lockvalue);
    }

    @Override
    public boolean relock(String lockname, String lockvalue, Long timeout, Long expire) {
        return  clientRouteSelector.determineCurrentClientBean().relock(lockname, lockvalue, timeout, expire);
    }

    @Override
    public boolean unrelock(String lockname, String lockvalue) {
        return  clientRouteSelector.determineCurrentClientBean().unrelock(lockname, lockvalue);
    }

    @Override
    public boolean unrelock(String lockname) {
        return  clientRouteSelector.determineCurrentClientBean().unrelock(lockname);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return  clientRouteSelector.determineCurrentClientBean().get(key, clazz);
    }

    @Override
    public <T> T get(String dir, String key, Class<T> clazz) {
        return  clientRouteSelector.determineCurrentClientBean().get(dir, key, clazz);
    }

    @Override
    public Object get(String key) {
        return  clientRouteSelector.determineCurrentClientBean().get(key);
    }

    @Override
    public byte[] get(byte[] key) {
        return  clientRouteSelector.determineCurrentClientBean().get(key);
    }

    @Override
    public Object get(String dir, String key) {
        return  clientRouteSelector.determineCurrentClientBean().get(dir, key);
    }

    @Override
    public Object getRawData(String key) {
        return  clientRouteSelector.determineCurrentClientBean().getRawData(key);
    }

    @Override
    public Object getRawData(String dir, String key) {
        return  clientRouteSelector.determineCurrentClientBean().getRawData(dir, key);
    }

    @Override
    public void put(String key, Object object, int expireTime, int maxSizePerObj) {
         clientRouteSelector.determineCurrentClientBean().put(key, object, expireTime, maxSizePerObj);
    }

    @Override
    public void put(String dir, String key, Object object, int expireTime, int maxSizePerObj) {
         clientRouteSelector.determineCurrentClientBean().put(dir, key, object, expireTime, maxSizePerObj);
    }

    @Override
    public void put(String key, Object object) {
         clientRouteSelector.determineCurrentClientBean().put(key, object);
    }

    @Override
    public void put(String dir, String key, Object object) {
         clientRouteSelector.determineCurrentClientBean().put(dir, key, object);
    }

    @Override
    public void put(String key, Object object, int expireTime) {
         clientRouteSelector.determineCurrentClientBean().put(key, object, expireTime);
    }

    @Override
    public void put(String dir, String key, Object object, int expireTime) {
         clientRouteSelector.determineCurrentClientBean().put(dir, key, object, expireTime);
    }

    @Override
    public boolean putIfNotExists(String key, Object object) {
        return  clientRouteSelector.determineCurrentClientBean().putIfNotExists(key, object);
    }

    @Override
    public boolean remove(String key) {
        return  clientRouteSelector.determineCurrentClientBean().remove(key);
    }

    @Override
    public boolean remove(String dir, String key) {
        return  clientRouteSelector.determineCurrentClientBean().remove(dir, key);
    }

    @Override
    public void removeAll() {
         clientRouteSelector.determineCurrentClientBean().removeAll();
    }

    @Override
    public void setExpireTime(String key, int expireTime) {
         clientRouteSelector.determineCurrentClientBean().setExpireTime(key, expireTime);
    }

    @Override
    public void setExpireTime(String dir, String key, int expireTime) {
         clientRouteSelector.determineCurrentClientBean().setExpireTime(dir, key, expireTime);
    }

    @Override
    public int getExpireTime(String key) {
        return  clientRouteSelector.determineCurrentClientBean().getExpireTime(key);
    }

    @Override
    public int getExpireTime(String dir, String key) {
        return  clientRouteSelector.determineCurrentClientBean().getExpireTime(dir, key);
    }

    @Override
    public long incr(String key) {
        return  clientRouteSelector.determineCurrentClientBean().incr(key);
    }

    @Override
    public long incr(String dir, String key) {
        return  clientRouteSelector.determineCurrentClientBean().incr(dir, key);
    }

    @Override
    public long incr(String key, long value) {
        return  clientRouteSelector.determineCurrentClientBean().incr(key, value);
    }

    @Override
    public long incr(String dir, String key, long value) {
        return  clientRouteSelector.determineCurrentClientBean().incr(dir, key, value);
    }

    @Override
    public long decr(String key) {
        return  clientRouteSelector.determineCurrentClientBean().decr(key);
    }

    @Override
    public long decr(String dir, String key) {
        return  clientRouteSelector.determineCurrentClientBean().decr(dir, key);
    }

    @Override
    public long decr(String key, long value) {
        return  clientRouteSelector.determineCurrentClientBean().decr(key, value);
    }

    @Override
    public long decr(String dir, String key, long value) {
        return  clientRouteSelector.determineCurrentClientBean().decr(dir, key, value);
    }

    @Override
    public int getMaxKeyLen() {
        return  clientRouteSelector.determineCurrentClientBean().getMaxKeyLen();
    }

    @Override
    public List<String> listKeys(String dir, List<String> excludeSubDirs, String keyWord, boolean refresh, int offset, int limit) {
        return  clientRouteSelector.determineCurrentClientBean().listKeys(dir, excludeSubDirs, keyWord, refresh, offset, limit);
    }

    @Override
    public List<String> listKeys(String dir, List<String> excludeSubDirs, String keyWord, int offset, int limit) {
        return  clientRouteSelector.determineCurrentClientBean().listKeys(dir, excludeSubDirs, keyWord, offset, limit);
    }

    @Override
    public long getKeyCount(String dir, String key) {
        return  clientRouteSelector.determineCurrentClientBean().getKeyCount(dir, key);
    }

    @Override
    public long getKeyCount(String key) {
        return  clientRouteSelector.determineCurrentClientBean().getKeyCount(key);
    }

    @Override
    public void removeKeyCount(String dir, String key) {
         clientRouteSelector.determineCurrentClientBean().removeKeyCount(dir, key);
    }

    @Override
    public void removeKeyCount(String key) {
         clientRouteSelector.determineCurrentClientBean().removeKeyCount(key);
    }

    @Override
    public void sadd(String key, String member) {
         clientRouteSelector.determineCurrentClientBean().sadd(key, member);
    }

    @Override
    public void sadd(String key, List<String> members) {
         clientRouteSelector.determineCurrentClientBean().sadd(key, members);
    }

    @Override
    public void srem(String key, String member) {
         clientRouteSelector.determineCurrentClientBean().srem(key, member);
    }

    @Override
    public Collection<String> getMembers(String key) {
        return  clientRouteSelector.determineCurrentClientBean().getMembers(key);
    }

    @Override
    public List<KeyCount> listTopCount(String dir, int sortFlag, int offset, int limit) {
        return  clientRouteSelector.determineCurrentClientBean().listTopCount(dir, sortFlag, offset, limit);
    }

    @Override
    public List<KeyCount> listTopCount(String dir, int sortFlag, boolean refresh, int offset, int limit) {
        return  clientRouteSelector.determineCurrentClientBean().listTopCount(dir, sortFlag, refresh, offset, limit);
    }

    @Override
    public void batchRemove(String dir, List<String> keyList) {
         clientRouteSelector.determineCurrentClientBean().batchRemove(dir, keyList);
    }

    @Override
    public String[] getServers() {
        return  clientRouteSelector.determineCurrentClientBean().getServers();
    }

    @Override
    public void destroy() {
         clientRouteSelector.determineCurrentClientBean().destroy();
    }

    @Override
    public boolean isAlive(String key) {
        return  clientRouteSelector.determineCurrentClientBean().isAlive(key);
    }

    @Override
    public boolean isAlive(String dir, String key) {
        return  clientRouteSelector.determineCurrentClientBean().isAlive(dir, key);
    }

    @Override
    public String getCacheBackend() {
        return  clientRouteSelector.determineCurrentClientBean().getCacheBackend();
    }

    @Override
    public ICache getCurrentCache() {
        return  clientRouteSelector.determineCurrentClientBean().getCurrentCache();
    }
}
