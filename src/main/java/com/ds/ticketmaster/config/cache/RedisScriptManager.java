package com.ds.ticketmaster.config.cache;

import com.ds.ticketmaster.constant.Constant;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RedisScriptManager {

    private final Map<String, RedisScript<?>> scripts = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        registerScript(Constant.LuaScript.JOIN_QUEUE_SCRIPT, "scripts/join_queue.lua", String.class);
        registerScript(Constant.LuaScript.GET_QUEUE_POSITION, "scripts/get_queue_position.lua", String.class);
    }

    private <T> void registerScript(String name, String path, Class<T> returnType) {
        RedisScript<T> script = RedisScript.of(new ClassPathResource(path), returnType);
        scripts.put(name, script);
    }

    @SuppressWarnings("unchecked")
    public <T> RedisScript<T> getScript(String name) {
        RedisScript<T> script = (RedisScript<T>) scripts.get(name);
        if (script == null) {
            throw new IllegalArgumentException("Script not found: " + name);
        }
        return script;
    }

}
