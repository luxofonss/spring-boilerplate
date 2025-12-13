package com.ds.ticketmaster.config.health;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.data.redis.RedisHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component("redisHealthIndicator")
@Slf4j
public class RedisHealthCheck extends RedisHealthIndicator {

    public RedisHealthCheck(RedisConnectionFactory redisConnectionFactory) {
        super(redisConnectionFactory);
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            super.doHealthCheck(builder);
            log.info(String.format("REDIS: %s", builder.build().getStatus()));
        } catch (Exception e) {
            log.info("REDIS : DOWN");
            log.info("Exception: ", e);
        }
    }
}
