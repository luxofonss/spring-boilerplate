package com.ds.ticketmaster.service.redis;


import com.ds.ticketmaster.config.cache.RedisCacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {

    @Autowired
    protected RedissonClient redissonClient;

    @Autowired
    private RedisCacheProperties redisCacheProperties;

    @Autowired
    @Qualifier("redisTemplateWithStringKey")
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("redisTemplateWithStringKeyStringValue")
    RedisTemplate<String, Object> redisTemplateForStringKeyStringValue;

    @Autowired
    @Qualifier("cacheThreadPool")
    Executor executor;

    @Autowired
    ObjectMapper objectMapper;

    @SneakyThrows
    public RLock getLockByKey(String key) {
        RLock lock = redissonClient.getLock(key);
        log.info("getLockByKey {}", key);

        if (lock.tryLock(redisCacheProperties.getRedissonAcquisitionTime(), redisCacheProperties.getRedissonUnlockAfterTime(), TimeUnit.MILLISECONDS)) {
            return lock;
        } else {
            return null;
        }
    }

    @SneakyThrows
    public RLock getLockByKey(String key, Integer waitTime, Integer leaseTime) {
        RLock lock = redissonClient.getLock(key);
        log.info("getLockByKey {}", key);

        if (lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS)) {
            return lock;
        } else {
            return null;
        }
    }

    public void unLock(RLock lock, String key) {
        try {
            log.info("unlock key {}", key);
            lock.unlock();
        } catch (Exception e) {
            log.error("unLock exception error: {}, key {}", e.getMessage(), key);
        }
    }

    //* put data to Redis, with a specific expire time
    public void setValueExpiresToRedis(String key, Object value, long seconds) {
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
        log.info("set key-value pair to Redis, with key: {}, value: {}, ttl: {} seconds", key, value, seconds);
    }

    public void setStringKeyStringValueToRedis(String key, Object value, long seconds) {
        redisTemplateForStringKeyStringValue.opsForValue().set(key, value);
        redisTemplateForStringKeyStringValue.expire(key, seconds, TimeUnit.SECONDS);
        log.info("set key-value pair to Redis, with key: {}, value: {}, ttl: {} seconds", key, value, seconds);
    }

    public void setObjectValueExpiresToRedis(String key, String field, Map<String, Object> value, long seconds) {
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);

    }

    //* get the value from key in Redis
    public <T> T getValueToRedis(String key, Class<T> clazz) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    //* update the new value for key in Redis
    public void updateValueInRedis(String key, Object newValue, long seconds) {
        redisTemplate.opsForValue().set(key, newValue);
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    public boolean updateSetValue(String key, String oldValue, String newValue) {
        redisTemplate.opsForSet().remove(key, oldValue);
        redisTemplate.opsForSet().add(key, newValue);
        return true;
    }

    public Set<String> getKeysByPattern(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(redisCacheProperties.getScanCount()).build();

        Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(redisConnection ->
                redisConnection.scan(options));

        while (cursor.hasNext()) {
            keys.add(new String(cursor.next()));
        }

        cursor.close();
        return keys;
    }

    public <T> T getObjectValueExpiresToRedis(String key, String hashKey, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    public void removeRedisKey(String key) {
        redisTemplate.delete(key);
        log.info("removed key: {} from Redis", key);
    }

}

