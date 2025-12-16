package com.ds.ticketmaster.config.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisScriptConfiguration {

    @Bean
    public RedisScript<Long> joinQueueScript() {
        // LUA script
        String script = """
            -- KEYS[1]: counterKey (total:ingestion_count:{eventId})
            -- KEYS[2]: sessionKey (session:{eventId}:{userId})
            -- ARGV[1]: sessionCache object (serialized JSON string)
            -- ARGV[2]: defaultSessionTTL (seconds)
            
            local setnx_result = redis.call('SETNX', KEYS[2], ARGV[1])
            
            if setnx_result == 1 then
                -- Key not exiss, SETNX success (new user)
                -- 1. set TTL
                redis.call('EXPIRE', KEYS[2], ARGV[2])
                -- 2. increase order number
                local ticketNumber = redis.call('INCR', KEYS[1])
            
                return ticketNumber
            else
                -- Key existed, SETNX fail
                return 0
            end
            """;

        return new DefaultRedisScript<>(script, Long.class);
    }

}
