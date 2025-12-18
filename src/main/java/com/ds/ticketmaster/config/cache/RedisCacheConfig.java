package com.ds.ticketmaster.config.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ReadFrom;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableConfigurationProperties(RedisCacheProperties.class)
@ConditionalOnProperty(value = "app.cache.redis.enable", havingValue = "true")
public class RedisCacheConfig {

    @Value("${spring.application.name:tm}")
    private String applicationShortName;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisCacheProperties properties) {
        String password = StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : null;
        Config config = new Config();
        RedissonClient redisson = null;

        try {
            if (!CollectionUtils.isEmpty(properties.getNodes())) {
                List<String> nodes = properties.getNodes().stream()
                        .map(node -> properties.getRedissonSSL() + node)
                        .toList();

                config.useClusterServers()
                        .setPassword(password)
                        .addNodeAddress(nodes.toArray(new String[0]))
                        .setConnectTimeout(properties.getClusterConnectTimeout())
                        .setTimeout((int) (properties.getTimeoutSeconds() * 1000))
                        .setRetryAttempts(properties.getClusterRetryAttempts())
                        .setRetryInterval(properties.getClusterRetryInterval())
                        .setMasterConnectionPoolSize(properties.getMasterConnectionPoolSize())
                        .setSlaveConnectionPoolSize(properties.getSlaveConnectionPoolSize())
                        .setMasterConnectionMinimumIdleSize(properties.getMinIdle().intValue())
                        .setSlaveConnectionMinimumIdleSize(properties.getMinIdle().intValue())
                        .setReadMode(ReadMode.SLAVE);
            } else {
                config.useSingleServer()
                        .setPassword(password)
                        .setConnectionPoolSize(properties.getMasterConnectionPoolSize())
                        .setConnectionMinimumIdleSize(properties.getMinIdle().intValue())
                        .setTimeout((int) (properties.getTimeoutSeconds() * 1000))
                        .setRetryAttempts(properties.getClusterRetryAttempts())
                        .setRetryInterval(properties.getClusterRetryInterval())
                        .setAddress(properties.getRedissonSSL() + properties.getHost() + ":" + properties.getPort());
            }
            redisson = Redisson.create(config);
            log.info("Redisson initialized successfully");
            return redisson;
        } catch (Exception e) {
            log.error("Failed to initialize Redisson client", e);
            if (redisson != null) {
                try {
                    redisson.shutdown();
                } catch (Exception shutdownEx) {
                    log.warn("Error when try shutdown Redisson after failure", shutdownEx);
                }
            }
            throw new IllegalStateException("Cannot initialize Redisson", e);
        }
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(RedisCacheProperties properties) {
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(buildPoolConfig(properties))
                        .commandTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .shutdownTimeout(Duration.ofMillis(properties.getShutdownTimeoutMillis()));

        LettuceConnectionFactory factory;

        if (!CollectionUtils.isEmpty(properties.getNodes())) {
            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(properties.getNodes());
            if (StringUtils.hasText(properties.getPassword())) {
                clusterConfig.setPassword(RedisPassword.of(properties.getPassword()));
            }
            builder.readFrom(ReadFrom.REPLICA_PREFERRED);
            factory = new LettuceConnectionFactory(clusterConfig, builder.build());
        } else {
            RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
            standaloneConfig.setHostName(properties.getHost());
            standaloneConfig.setPort(properties.getPort());
            if (StringUtils.hasText(properties.getPassword())) {
                standaloneConfig.setPassword(RedisPassword.of(properties.getPassword()));
            }
            factory = new LettuceConnectionFactory(standaloneConfig, builder.build());
        }

        // Initialize and test connection
        factory.afterPropertiesSet();
        try (RedisConnection conn = factory.getConnection()) {
            conn.ping();
            log.info("Redis connection validated successfully");
        } catch (Exception e) {
            log.error("Failed to validate Redis connection", e);
            throw new IllegalStateException("Redis connection validation failed", e);
        }
        return factory;
    }

    private GenericObjectPoolConfig<?> buildPoolConfig(RedisCacheProperties properties) {
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        int maxTotal = properties.getMaxTotal() != null ? properties.getMaxTotal().intValue() : 8;
        int maxIdle = properties.getMaxIdle() != null ? properties.getMaxIdle().intValue() : 8;
        int minIdle = properties.getMinIdle() != null ? properties.getMinIdle().intValue() : 0;
        // Validate relationships
        maxIdle = Math.min(maxIdle, maxTotal);
        minIdle = Math.min(minIdle, maxIdle);
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(Boolean.TRUE.equals(properties.getTestOnBorrow()));
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(60));
        if (properties.getMaxWaitMillis() != null) {
            poolConfig.setMaxWait(Duration.ofMillis(properties.getMaxWaitMillis()));
        }
        return poolConfig;
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    @Qualifier(value = "redisTemplateWithStringKey")
    public RedisTemplate<String, Object> redisTemplateWithStringKey(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(stringSerializer);
        redisTemplate.setDefaultSerializer(stringSerializer);
        return redisTemplate;
    }

    @Bean
    @Qualifier(value = "redisTemplateWithStringKeyStringValue")
    public RedisTemplate<String, Object> redisTemplateWithStringKeyStringValue(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration(RedisCacheProperties properties) {
        return createCacheConfiguration(properties.getTimeoutSeconds());
    }

    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                          RedisCacheProperties properties) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        for (Map.Entry<String, Long> entry : properties.getCacheExpirations().entrySet()) {
            cacheConfigurations.put(entry.getKey(), createCacheConfiguration(entry.getValue()));
        }

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration(properties))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private RedisCacheConfiguration createCacheConfiguration(long timeoutInSeconds) {
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues()
                .computePrefixWith(cacheName ->
                        applicationShortName + "::" +
                                (StringUtils.hasText(cacheName) ? cacheName + "::" : ""))
                .entryTtl(Duration.ofSeconds(timeoutInSeconds));
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}