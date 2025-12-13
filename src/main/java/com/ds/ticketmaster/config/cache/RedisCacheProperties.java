package com.ds.ticketmaster.config.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.cache.redis")
public class RedisCacheProperties {
    private boolean enable = false;
    private long timeoutSeconds = 600;
    private int port = 6379;
    private String host = "localhost";
    private List<String> nodes = new ArrayList<>();
    private Map<String, Long> cacheExpirations = new HashMap<>();
    private String password = "";
    private String redissonSSL = "redis://";
    private Long maxTotal = 1000L;
    private Long maxIdle = 100L;
    private Long minIdle = 10L;
    private Long maxWaitMillis = 3000L;
    private Integer clusterConnectTimeout = 10000;
    private Integer clusterRetryInterval = 1500;
    private Integer clusterRetryAttempts = 3;
    private Integer shutdownTimeoutMillis = 100;
    private Integer masterConnectionPoolSize = 1000;
    private Integer slaveConnectionPoolSize = 1000;
    private Boolean testOnBorrow = true;
    private Integer scanCount = 5;
    private Integer redissonAcquisitionTime = 3000;
    private Integer redissonUnlockAfterTime = 10000;
}
