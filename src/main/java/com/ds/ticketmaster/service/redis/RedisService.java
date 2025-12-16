package com.ds.ticketmaster.service.redis;

import com.ds.ticketmaster.config.cache.RedisCacheProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {

    private final RedissonClient redissonClient;
    private final RedisCacheProperties redisCacheProperties;

    @Qualifier("redisTemplateWithStringKey")
    private final RedisTemplate<String, Object> redisTemplate;

    @Qualifier("redisTemplateWithStringKeyStringValue")
    private final RedisTemplate<String, Object> redisTemplateForStringKeyStringValue;

    @Qualifier("cacheThreadPool")
    private final Executor executor;

    private final ObjectMapper objectMapper;

    private final RedisScript<Long> joinQueueScript;

    // --- Locking Operations ---

    /**
     * Try to acquire a distributed lock using default timeout settings.
     *
     * @param key the lock key
     * @return RLock if acquired, or null if failed.
     */
    public RLock acquireLock(String key) {
        return acquireLock(key, redisCacheProperties.getRedissonAcquisitionTime(), redisCacheProperties.getRedissonUnlockAfterTime());
    }

    /**
     * Try to acquire a distributed lock with custom timeout.
     *
     * @param waitTime  Max wait time in milliseconds
     * @param leaseTime Lease time in milliseconds (auto unlock after this)
     * @return RLock instance if successful, or null if lock could not be acquired.
     */
    public RLock acquireLock(String key, Integer waitTime, Integer leaseTime) {
        RLock lock = redissonClient.getLock(key);
        if (log.isDebugEnabled()) {
            log.debug("Attempting to acquire lock: {}", key);
        }

        try {
            boolean isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            if (isLocked) {
                return lock;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for lock: {}", key);
        } catch (Exception e) {
            log.error("Error acquiring lock: {}", key, e);
        }
        return null;
    }

    public void unlock(RLock lock, String key) {
        if (lock == null) return;
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("Error unlocking key: {}", key, e);
        }
    }

    // --- Value Operations ---

    /**
     * Set value with TTL.
     */
    public void set(String key, Object value, long ttlInSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlInSeconds, TimeUnit.SECONDS);
        if (log.isDebugEnabled()) {
            log.debug("Set Redis key: {}, ttl: {}s", key, ttlInSeconds);
        }
    }

    /**
     * Set string value specifically using the string template.
     */
    public void setString(String key, Object value, long ttlInSeconds) {
        redisTemplateForStringKeyStringValue.opsForValue().set(key, value, ttlInSeconds, TimeUnit.SECONDS);
        if (log.isDebugEnabled()) {
            log.debug("Set Redis String key: {}, ttl: {}s", key, ttlInSeconds);
        }
    }

    /**
     * Get value and cast to target class.
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        return convertValue(value, clazz);
    }

    // --- Hash Operations ---

    /**
     * Get value from a Hash map.
     */
    public <T> T getHash(String key, String hashKey, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        return convertValue(value, clazz);
    }

    public void putHash(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    // --- Common Operations ---

    public void delete(String key) {
        redisTemplate.delete(key);
        log.info("Deleted Redis key: {}", key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * Set value only if key does not exist (Atomic SETNX).
     *
     * @throws IllegalStateException if key already exists.
     */
    public void setIfAbsent(String key, Object value, long ttlInSeconds) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, ttlInSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(success)) {
            throw new IllegalStateException("Key already exists in Redis: " + key);
        }
        if (log.isDebugEnabled()) {
            log.debug("SetIfAbsent Redis key: {}, ttl: {}s", key, ttlInSeconds);
        }
    }

    /**
     * Set String value only if key does not exist (Atomic SETNX).
     *
     * @throws IllegalStateException if key already exists.
     */
    public void setStringIfAbsent(String key, Object value, long ttlInSeconds) {
        Boolean success = redisTemplateForStringKeyStringValue.opsForValue().setIfAbsent(key, value, ttlInSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(success)) {
            throw new IllegalStateException("Key already exists in Redis: " + key);
        }
        if (log.isDebugEnabled()) {
            log.debug("SetStringIfAbsent Redis String key: {}, ttl: {}s", key, ttlInSeconds);
        }
    }

    /*
    Increase atomic
     */
    public Long increaseKeyBy(String key, Long value) {
        return redisTemplate.opsForValue().increment(key, value);
    }

    public Long executeJoinQueueScript(String counterKey, String sessionKey, Object sessionCache, Long ttl) {
        Object serializedSessionCache = redisTemplate.getValueSerializer().serialize(sessionCache);
        return redisTemplate.execute(
                joinQueueScript,
                List.of(counterKey, sessionKey), // KEYS[1], KEYS[2]
                serializedSessionCache,           // ARGV[1]
                ttl.toString()                    // ARGV[2]
        );
    }

    public <T> void setHash(String key, T object, long timeout) {
        if (object == null) {
            return;
        }

        Map<String, Object> properties = objectMapper.convertValue(object,
                new TypeReference<Map<String, Object>>() {
                });

        redisTemplate.opsForHash().putAll(key, properties);

        // Đặt TTL cho Key chính
        redisTemplate.expire(key, Duration.ofSeconds(timeout));
    }

    public <T> T getHash(String key, Class<T> clazz) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries == null || entries.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.convertValue(entries, clazz);
        } catch (IllegalArgumentException e) {
            log.error("Error converting Redis Hash to Java Class {}", clazz.getName(), e);
            return null;
        }
    }

    public void updateHashField(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * Helper to safely cast/convert objects.
     */
    private <T> T convertValue(Object value, Class<T> clazz) {
        if (value == null) return null;
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        try {
            return objectMapper.convertValue(value, clazz);
        } catch (IllegalArgumentException e) {
            log.warn("Could not convert value of type {} to {}", value.getClass().getName(), clazz.getName());
            return null;
        }
    }

}
