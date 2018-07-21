package com.al.crm.redis.cluster;

import com.al.crm.nosql.cache.ICache;
import com.al.crm.nosql.cache.IRedisFix;
import com.al.crm.nosql.cache.KeyCount;
import redis.clients.jedis.BinaryClient;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractIRedisFixProxy implements IRedisFix {

    protected abstract IRedisFix determineCurrentRedisSource();

    @Override
    public boolean delkey(String key) {
        return determineCurrentRedisSource().delkey(key);
    }

    @Override
    public long ttl(String key) {
        return determineCurrentRedisSource().ttl(key);
    }

    @Override
    public long pttl(String key) {
        return determineCurrentRedisSource().pttl(key);
    }

    @Override
    public long setnx(String key, String value) {
        return determineCurrentRedisSource().setnx(key, value);
    }

    @Override
    public Long expire(byte[] key, int seconds) {
        return determineCurrentRedisSource().expire(key, seconds);
    }

    @Override
    public boolean exists(String key) {
        return determineCurrentRedisSource().exists(key);
    }

    @Override
    public Long hset(String hash, String key, String value) {
        return determineCurrentRedisSource().hset(hash, key, value);
    }

    @Override
    public Long hsetnx(String hash, String key, String value) {
        return determineCurrentRedisSource().hsetnx(hash, key, value);
    }

    @Override
    public Long hset(byte[] hash, byte[] key, byte[] value) {
        return determineCurrentRedisSource().hset(hash, key, value);
    }

    @Override
    public Long hsetPojo(String hash, String key, Object pojo) throws IOException {
        return determineCurrentRedisSource().hsetPojo(hash, key, pojo);
    }

    @Override
    public Object hgetPojo(String hash, String key) {
        return determineCurrentRedisSource().hgetPojo(hash, key);
    }

    @Override
    public String hget(String hash, String key) {
        return determineCurrentRedisSource().hget(hash, key);
    }

    @Override
    public byte[] hget(byte[] hash, byte[] key) {
        return determineCurrentRedisSource().hget(hash, key);
    }

    @Override
    public Boolean hexists(byte[] hash, byte[] key) {
        return determineCurrentRedisSource().hexists(hash, key);
    }

    @Override
    public Boolean hexists(String hash, String key) {
        return determineCurrentRedisSource().hexists(hash, key);
    }

    @Override
    public Long hincrby(byte[] hash, byte[] key, Long value) {
        return determineCurrentRedisSource().hincrby(hash, key, value);
    }

    @Override
    public Long hincrby(String hash, String key, Long value) {
        return determineCurrentRedisSource().hincrby(hash, key, value);
    }

    @Override
    public Long hlen(byte[] hash) {
        return determineCurrentRedisSource().hlen(hash);
    }

    @Override
    public Long hlen(String hash) {
        return determineCurrentRedisSource().hlen(hash);
    }

    @Override
    public Long hdel(byte[] hash, byte[] key) {
        return determineCurrentRedisSource().hdel(hash, key);
    }

    @Override
    public Long hdel(String hash, String key) {
        return determineCurrentRedisSource().hdel(hash, key);
    }

    @Override
    public Set<String> hkeys(byte[] hash) {
        return determineCurrentRedisSource().hkeys(hash);
    }

    @Override
    public Set<String> hkeys(String hash) {
        return determineCurrentRedisSource().hkeys(hash);
    }

    @Override
    public List<byte[]> hvals(byte[] hash) {
        return determineCurrentRedisSource().hvals(hash);
    }

    @Override
    public List<byte[]> hvals(String hash) {
        return determineCurrentRedisSource().hvals(hash);
    }

    @Override
    public Map<byte[], byte[]> hgetall(byte[] hash) {
        return determineCurrentRedisSource().hgetall(hash);
    }

    @Override
    public Map<String, byte[]> hgetall(String hash) {
        return determineCurrentRedisSource().hgetall(hash);
    }

    @Override
    public Map<String, Object> hgetallPojo(String hash) {
        return determineCurrentRedisSource().hgetallPojo(hash);
    }

    @Override
    public Long lpush(String key, String... value) {
        return determineCurrentRedisSource().lpush(key, value);
    }

    @Override
    public Long lpush(byte[] key, byte[]... value) {
        return determineCurrentRedisSource().lpush(key, value);
    }

    @Override
    public Long rpush(String key, String... value) {
        return determineCurrentRedisSource().rpush(key, value);
    }

    @Override
    public Long rpush(byte[] key, byte[]... value) {
        return determineCurrentRedisSource().rpush(key, value);
    }

    @Override
    public Long rpush(String key, Object... values) {
        return determineCurrentRedisSource().rpush(key, values);
    }

    @Override
    public Long lpush(String key, Object... values) {
        return determineCurrentRedisSource().lpush(key, values);
    }

    @Override
    public Long linsert(String key, BinaryClient.LIST_POSITION position, String pivot, String value) {
        return determineCurrentRedisSource().linsert(key, position, pivot, value);
    }

    @Override
    public Long linsert(byte[] key, BinaryClient.LIST_POSITION position, byte[] pivot, byte[] value) {
        return determineCurrentRedisSource().linsert(key, position, pivot, value);
    }

    @Override
    public boolean lset(String key, long index, String value) {
        return determineCurrentRedisSource().lset(key, index, value);
    }

    @Override
    public boolean lset(byte[] key, long index, byte[] value) {
        return determineCurrentRedisSource().lset(key, index, value);
    }

    @Override
    public Long lrem(String key, long count, String value) {
        return determineCurrentRedisSource().lrem(key, count, value);
    }

    @Override
    public Long lrem(byte[] key, long count, byte[] value) {
        return determineCurrentRedisSource().lrem(key, count, value);
    }

    @Override
    public boolean ltrim(String key, long start, long end) {
        return determineCurrentRedisSource().ltrim(key, start, end);
    }

    @Override
    public boolean ltrim(byte[] key, long start, long end) {
        return determineCurrentRedisSource().ltrim(key, start, end);
    }

    @Override
    public byte[] lpop(byte[] key) {
        return determineCurrentRedisSource().lpop(key);
    }

    @Override
    public String lpop_ori(String key) {
        return determineCurrentRedisSource().lpop_ori(key);
    }

    @Override
    public String rpop(String key) {
        return determineCurrentRedisSource().rpop(key);
    }

    @Override
    public byte[] rpop(byte[] key) {
        return determineCurrentRedisSource().rpop(key);
    }

    @Override
    public List<String> blpop(int timeout, String... keys) {
        return determineCurrentRedisSource().blpop(timeout, keys);
    }

    @Override
    public List<byte[]> blpop(int timeout, byte[]... keys) {
        return determineCurrentRedisSource().blpop(timeout, keys);
    }

    @Override
    public List<String> brpop(int timeout, String... keys) {
        return determineCurrentRedisSource().brpop(timeout, keys);
    }

    @Override
    public List<byte[]> brpop(int timeout, byte[]... keys) {
        return determineCurrentRedisSource().brpop(timeout, keys);
    }

    @Override
    public String lindex(String key, long index) {
        return determineCurrentRedisSource().lindex(key, index);
    }

    @Override
    public byte[] lindex(byte[] key, long index) {
        return determineCurrentRedisSource().lindex(key, index);
    }

    @Override
    public long llen(String key) {
        return determineCurrentRedisSource().llen(key);
    }

    @Override
    public long llen(byte[] key) {
        return determineCurrentRedisSource().llen(key);
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        return determineCurrentRedisSource().lrange(key, start, end);
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        return determineCurrentRedisSource().lrange(key, start, end);
    }

    @Override
    public Boolean sadd(byte[] key, byte[]... members) {
        return determineCurrentRedisSource().sadd(key, members);
    }

    @Override
    public Boolean srem(byte[] key, byte[]... members) {
        return determineCurrentRedisSource().srem(key, members);
    }

    @Override
    public String spop(String key) {
        return determineCurrentRedisSource().spop(key);
    }

    @Override
    public byte[] spop(byte[] key) {
        return determineCurrentRedisSource().spop(key);
    }

    @Override
    public long scard(String key) {
        return determineCurrentRedisSource().scard(key);
    }

    @Override
    public long scard(byte[] key) {
        return determineCurrentRedisSource().scard(key);
    }

    @Override
    public Boolean sismember(String key, String member) {
        return determineCurrentRedisSource().sismember(key, member);
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        return determineCurrentRedisSource().sismember(key, member);
    }

    @Override
    public String srandmember(String key) {
        return determineCurrentRedisSource().srandmember(key);
    }

    @Override
    public byte[] srandmember(byte[] key) {
        return determineCurrentRedisSource().srandmember(key);
    }

    @Override
    public Set<String> smembers(String key) {
        return determineCurrentRedisSource().smembers(key);
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        return determineCurrentRedisSource().smembers(key);
    }

    @Override
    public String type(String key) {
        return determineCurrentRedisSource().type(key);
    }

    @Override
    public String type(byte[] key) {
        return determineCurrentRedisSource().type(key);
    }

    @Override
    public String set(String key, String value, String nxxx, String expx, int time) {
        return determineCurrentRedisSource().set(key, value, nxxx, expx, time);
    }

    @Override
    public String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        return determineCurrentRedisSource().set(key, value, nxxx, expx, time);
    }

    @Override
    public String setex(String key, int timeout, String value) {
        return determineCurrentRedisSource().setex(key, timeout, value);
    }

    @Override
    public String setex(byte[] key, int timeout, byte[] value) {
        return determineCurrentRedisSource().setex(key, timeout, value);
    }

    @Override
    public String set(byte[] key, byte[] value) {
        return determineCurrentRedisSource().set(key, value);
    }

    @Override
    public String set(String key, String value) {
        return determineCurrentRedisSource().set(key, value);
    }

    @Override
    public boolean lock(String lockname, String lockvalue, Long timeout, Long expire) {
        return determineCurrentRedisSource().lock(lockname, lockvalue, timeout, expire);
    }

    @Override
    public boolean lock(String lockname, String lockvalue, int timeout, int expire) {
        return determineCurrentRedisSource().lock(lockname, lockvalue, timeout, expire);
    }

    @Override
    public boolean unlock(String lockname, String lockvalue) {
        return determineCurrentRedisSource().unlock(lockname, lockvalue);
    }

    @Override
    public boolean relock(String lockname, String lockvalue, Long timeout, Long expire) {
        return determineCurrentRedisSource().relock(lockname, lockvalue, timeout, expire);
    }

    @Override
    public boolean unrelock(String lockname, String lockvalue) {
        return determineCurrentRedisSource().unrelock(lockname, lockvalue);
    }

    @Override
    public boolean unrelock(String lockname) {
        return determineCurrentRedisSource().unrelock(lockname);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return determineCurrentRedisSource().get(key, clazz);
    }

    @Override
    public <T> T get(String dir, String key, Class<T> clazz) {
        return determineCurrentRedisSource().get(dir, key, clazz);
    }

    @Override
    public Object get(String key) {
        return determineCurrentRedisSource().get(key);
    }

    @Override
    public byte[] get(byte[] key) {
        return determineCurrentRedisSource().get(key);
    }

    @Override
    public Object get(String dir, String key) {
        return determineCurrentRedisSource().get(dir, key);
    }

    @Override
    public Object getRawData(String key) {
        return determineCurrentRedisSource().getRawData(key);
    }

    @Override
    public Object getRawData(String dir, String key) {
        return determineCurrentRedisSource().getRawData(dir, key);
    }

    @Override
    public void put(String key, Object object, int expireTime, int maxSizePerObj) {
        determineCurrentRedisSource().put(key, object, expireTime, maxSizePerObj);
    }

    @Override
    public void put(String dir, String key, Object object, int expireTime, int maxSizePerObj) {
        determineCurrentRedisSource().put(dir, key, object, expireTime, maxSizePerObj);
    }

    @Override
    public void put(String key, Object object) {
        determineCurrentRedisSource().put(key, object);
    }

    @Override
    public void put(String dir, String key, Object object) {
        determineCurrentRedisSource().put(dir, key, object);
    }

    @Override
    public void put(String key, Object object, int expireTime) {
        determineCurrentRedisSource().put(key, object, expireTime);
    }

    @Override
    public void put(String dir, String key, Object object, int expireTime) {
        determineCurrentRedisSource().put(dir, key, object, expireTime);
    }

    @Override
    public boolean putIfNotExists(String key, Object object) {
        return determineCurrentRedisSource().putIfNotExists(key, object);
    }

    @Override
    public boolean remove(String key) {
        return determineCurrentRedisSource().remove(key);
    }

    @Override
    public boolean remove(String dir, String key) {
        return determineCurrentRedisSource().remove(dir, key);
    }

    @Override
    public void removeAll() {
        determineCurrentRedisSource().removeAll();
    }

    @Override
    public void setExpireTime(String key, int expireTime) {
        determineCurrentRedisSource().setExpireTime(key, expireTime);
    }

    @Override
    public void setExpireTime(String dir, String key, int expireTime) {
        determineCurrentRedisSource().setExpireTime(dir, key, expireTime);
    }

    @Override
    public int getExpireTime(String key) {
        return determineCurrentRedisSource().getExpireTime(key);
    }

    @Override
    public int getExpireTime(String dir, String key) {
        return determineCurrentRedisSource().getExpireTime(dir, key);
    }

    @Override
    public long incr(String key) {
        return determineCurrentRedisSource().incr(key);
    }

    @Override
    public long incr(String dir, String key) {
        return determineCurrentRedisSource().incr(dir, key);
    }

    @Override
    public long incr(String key, long value) {
        return determineCurrentRedisSource().incr(key, value);
    }

    @Override
    public long incr(String dir, String key, long value) {
        return determineCurrentRedisSource().incr(dir, key, value);
    }

    @Override
    public long decr(String key) {
        return determineCurrentRedisSource().decr(key);
    }

    @Override
    public long decr(String dir, String key) {
        return determineCurrentRedisSource().decr(dir, key);
    }

    @Override
    public long decr(String key, long value) {
        return determineCurrentRedisSource().decr(key, value);
    }

    @Override
    public long decr(String dir, String key, long value) {
        return determineCurrentRedisSource().decr(dir, key, value);
    }

    @Override
    public int getMaxKeyLen() {
        return determineCurrentRedisSource().getMaxKeyLen();
    }

    @Override
    public List<String> listKeys(String dir, List<String> excludeSubDirs, String keyWord, boolean refresh, int offset, int limit) {
        return determineCurrentRedisSource().listKeys(dir, excludeSubDirs, keyWord, refresh, offset, limit);
    }

    @Override
    public List<String> listKeys(String dir, List<String> excludeSubDirs, String keyWord, int offset, int limit) {
        return determineCurrentRedisSource().listKeys(dir, excludeSubDirs, keyWord, offset, limit);
    }

    @Override
    public long getKeyCount(String dir, String key) {
        return determineCurrentRedisSource().getKeyCount(dir, key);
    }

    @Override
    public long getKeyCount(String key) {
        return determineCurrentRedisSource().getKeyCount(key);
    }

    @Override
    public void removeKeyCount(String dir, String key) {
        determineCurrentRedisSource().removeKeyCount(dir, key);
    }

    @Override
    public void removeKeyCount(String key) {
        determineCurrentRedisSource().removeKeyCount(key);
    }

    @Override
    public void sadd(String key, String member) {
        determineCurrentRedisSource().sadd(key, member);
    }

    @Override
    public void sadd(String key, List<String> members) {
        determineCurrentRedisSource().sadd(key, members);
    }

    @Override
    public void srem(String key, String member) {
        determineCurrentRedisSource().srem(key, member);
    }

    @Override
    public Collection<String> getMembers(String key) {
        return determineCurrentRedisSource().getMembers(key);
    }

    @Override
    public List<KeyCount> listTopCount(String dir, int sortFlag, int offset, int limit) {
        return determineCurrentRedisSource().listTopCount(dir, sortFlag, offset, limit);
    }

    @Override
    public List<KeyCount> listTopCount(String dir, int sortFlag, boolean refresh, int offset, int limit) {
        return determineCurrentRedisSource().listTopCount(dir, sortFlag, refresh, offset, limit);
    }

    @Override
    public void batchRemove(String dir, List<String> keyList) {
        determineCurrentRedisSource().batchRemove(dir, keyList);
    }

    @Override
    public String[] getServers() {
        return determineCurrentRedisSource().getServers();
    }

    @Override
    public void destroy() {
        determineCurrentRedisSource().destroy();
    }

    @Override
    public boolean isAlive(String key) {
        return determineCurrentRedisSource().isAlive(key);
    }

    @Override
    public boolean isAlive(String dir, String key) {
        return determineCurrentRedisSource().isAlive(dir, key);
    }

    @Override
    public String getCacheBackend() {
        return determineCurrentRedisSource().getCacheBackend();
    }

    @Override
    public ICache getCurrentCache() {
        return determineCurrentRedisSource().getCurrentCache();
    }
}
