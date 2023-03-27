package com.bmdst.services.starter.redis.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.DefaultScriptExecutor;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * redis 工具类
 */
@Slf4j
public class RedisClient {
    private static final long FIRST_INDEX_OF_LIST = 0;
    private static final long LAST_INDEX_OF_LIST = -1;
    private static final long DEFAULT_LIVE_TIME = 3600;
    private static final long DEFAULT_INCREMENT = 1;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void delete(String... keys) {
        redisTemplate.delete(Arrays.asList(keys));
    }

    public boolean exists(String key) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection -> connection.exists(key.getBytes()));
    }

    public void flushCacheDb() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.flushDb();
            return null;
        });
    }

    public long cacheDbSize() {
        return redisTemplate.execute(RedisServerCommands::dbSize);
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    public Set<String> scan(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keysSet = new HashSet<>();
            Cursor<byte[]> cursor = connection.scan(new ScanOptions.ScanOptionsBuilder().match(pattern).count(Long.MAX_VALUE).build());
            while (cursor.hasNext()) {
                keysSet.add(new String(cursor.next()));
            }
            return keysSet;
        });
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public void set(String key, Object value, long liveTime) {
        redisTemplate.opsForValue().set(key, value, liveTime, TimeUnit.SECONDS);
    }

    public void set(String key, Object value, long liveTime, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, liveTime, timeUnit);
    }

    public void set(String key, Object value) {
        set(key, value, DEFAULT_LIVE_TIME);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void multiSet(Map<String, Object> map) {
        redisTemplate.opsForValue().multiSet(map);
    }

    public List<Object> multiGet(String... keys) {
        return redisTemplate.opsForValue().multiGet(Arrays.asList(keys));
    }

    public long increment(String key) {
        return redisTemplate.opsForValue().increment(key, DEFAULT_INCREMENT);
    }

    public long increment(String key, long offset) {
        return redisTemplate.opsForValue().increment(key, offset);
    }


    public long lPush(String key, Collection<Object> values) {
        return redisTemplate.opsForList().leftPushAll(key, values);
    }

    public List<Object> lRange(String key, long begin, long end) {
        return redisTemplate.opsForList().range(key, begin, end);
    }

    public List<Object> lRangeAll(String key) {
        return redisTemplate.opsForList().range(key, FIRST_INDEX_OF_LIST, LAST_INDEX_OF_LIST);
    }

    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    public Long size(String key) {
        return redisTemplate.opsForList().size(key);
    }

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public void hmSet(String key, Map<Object, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public Long hDel(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    public Long hIncrement(String key, String field, Integer increment) {
        return redisTemplate.opsForHash().increment(key, field, increment);
    }

    public boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public boolean convertAndSend(String channel, Object message) {
        try {
            redisTemplate.convertAndSend(channel, message);
        } catch (Exception e) {
            log.error("redis发布消息服务出现异常", e);
            return false;
        }
        return true;
    }

    public RedisSerializer getValueSerializer() {
        return redisTemplate.getValueSerializer();
    }

    /**
     * 这种属于快速失败的获取
     *
     * @param key
     * @param value
     * @param timeout 单位 秒
     * @return
     */
    public boolean lock(String key, String value, int timeout) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("lock error, key = {}, timeout = {}", key, timeout, e);
            return false;
        }
    }

    public boolean unlock(String key, String value) {
        try {
            //lua脚本(保证查询和删除的原子性)
            String luaScript = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
            DefaultRedisScript redisScript = new DefaultRedisScript(luaScript);
            redisScript.setResultType(Boolean.class);
            DefaultScriptExecutor defaultScriptExecutor = new DefaultScriptExecutor(redisTemplate);
            return (boolean) defaultScriptExecutor.execute(redisScript, Arrays.asList(key), value);
        } catch (Exception e) {
            log.error("unlock error, key= {} ", key, e);
            return false;
        }
    }


    /**
     * 阻塞等待获取并执行
     *
     * @param key         锁key
     * @param timeout     锁超时时间 单位 秒
     * @param voidExecute 执行逻辑
     */
    public void executeWithLock(String key, int timeout, VoidExecute voidExecute) {
        String value = UUID.randomUUID().toString();
        try {
            while (!lock(key, value, timeout)) {
                Thread.sleep(20);
            }
            voidExecute.execute();
        } catch (Exception e) {
            log.error("executeWithLock error, key={}, timeout={}", key, timeout, e);
        } finally {
            unlock(key, value);
        }
    }

    /**
     * 带尝试获取锁超时时间的锁操作
     *
     * @param key         锁标识
     * @param leaseTime   锁定时间
     * @param waitTime    尝试获取锁超时时间
     * @param voidExecute 执行逻辑
     * @return
     */
    public boolean executeWithTryLock(String key, int leaseTime, int waitTime, VoidExecute voidExecute) {
        String value = UUID.randomUUID().toString();
        try {
            long waitTimeout = TimeUnit.SECONDS.toMillis(waitTime);
            long begin = System.currentTimeMillis();
            while (!lock(key, value, leaseTime)) {
                if (System.currentTimeMillis() - begin >= waitTimeout) {
                    log.error("executeWithTryLock fail due to wait timeout! key={}, leaseTime={}, waitTime={}", key, leaseTime, waitTime);
                    return false;
                }
                Thread.sleep(20);
            }
            voidExecute.execute();
            return true;
        } catch (Exception e) {
            log.error("executeWithLock error, key={}, leaseTime={}", key, leaseTime, e);
            return false;
        } finally {
            unlock(key, value);
        }
    }

    public <T> T executeWithLock(String key, int timeout, ReturnExecute<T> returnExecute) {
        String value = UUID.randomUUID().toString();
        try {
            while (!lock(key, value, timeout)) {
                Thread.sleep(20);
            }
            return returnExecute.execute();
        } catch (Exception e) {
            log.error("executeWithLock error, key={}, timeout={}", key, timeout, e);
            return null;
        } finally {
            unlock(key, value);
        }
    }

    public <T> T executeWithTryLock(String key, int leaseTime, int waitTime, ReturnExecute<T> returnExecute) {
        String value = UUID.randomUUID().toString();
        try {
            long waitTimeout = TimeUnit.SECONDS.toMillis(waitTime);
            long begin = System.currentTimeMillis();
            while (!lock(key, value, leaseTime)) {
                if (System.currentTimeMillis() - begin >= waitTimeout) {
                    log.error("executeWithTryLock fail due to wait timeout! key={}, leaseTime={}, waitTime={}", key, leaseTime, waitTime);
                    return null;
                }
                Thread.sleep(20);
            }
            return returnExecute.execute();
        } catch (Exception e) {
            log.error("executeWithLock error, key={}, leaseTime={}", key, leaseTime, e);
            return null;
        } finally {
            unlock(key, value);
        }
    }

    /**
     * 方便使用lambada调用，不带返回值接口
     */
    public interface VoidExecute {
        void execute();
    }

    /**
     * 带返回值接口
     *
     * @param <T>
     */
    public interface ReturnExecute<T> {
        T execute();
    }

}
