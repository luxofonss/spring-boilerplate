package com.ds.ticketmaster.service.consumer;

import com.ds.ticketmaster.config.kafka.KafkaProducer;
import com.ds.ticketmaster.constant.Constant;
import com.ds.ticketmaster.dto.JoinInQueueMessageDTO;
import com.ds.ticketmaster.dto.JoinInQueueRequestDTO;
import com.ds.ticketmaster.dto.JoinInQueueResponseDTO;
import com.ds.ticketmaster.dto.LuaQueueResult;
import com.ds.ticketmaster.service.BookingService;
import com.ds.ticketmaster.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    @Qualifier("redisTemplateWithStringKey")
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaProducer kafkaProducer;

    @Override
    public JoinInQueueResponseDTO joinQueue(JoinInQueueRequestDTO request) {
        String luaScript = """
                local event_id = ARGV[1]
                local user_id = ARGV[2]
                
                local existing = redis.call('zrank', 'waiting_list:'..event_id, user_id)
                if existing then
                    return cjson.encode({
                        alreadyInQueue = true,
                        position = tonumber(existing)
                    })
                end
                
                redis.call('zadd', 'waiting_list:'..event_id, tonumber(redis.call('time')[1]), user_id)
                local sequence = redis.call('zrank', 'waiting_list:'..event_id, user_id)
                
                return cjson.encode({
                    alreadyInQueue = false,
                    position = tonumber(sequence)
                })
                """;

        String resultJson = redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, String.class),
                Collections.emptyList(),
                request.getEventId(),
                request.getUserId().toString()
        );

        LuaQueueResult result = JsonUtils.readObject(resultJson, LuaQueueResult.class);

        if (result == null) {
            throw new RuntimeException("Failed to parse Redis response");
        }

        boolean alreadyInQueue = result.getAlreadyInQueue();
        Long sequence = result.getPosition();

        if (!alreadyInQueue) {
            JoinInQueueMessageDTO message = JoinInQueueMessageDTO.builder()
                    .eventId(request.getEventId())
                    .userId(request.getUserId())
                    .sequence(sequence)
                    .timestamp(System.currentTimeMillis())
                    .build();
            kafkaProducer.produce(Constant.TOPICS.BOOKING_REQUEST_TOPIC, message);
        }
        return JoinInQueueResponseDTO.builder()
                .alreadyInQueue(alreadyInQueue)
                .sequence(sequence)
                .build();
    }

    @Override
    public JoinInQueueResponseDTO getQueuePosition(String eventId, Long userId) {
        String luaScript = """
                local event_id = ARGV[1]
                local user_id = ARGV[2]
                
                local rank = redis.call('zrank', 'waiting_list:'..event_id, user_id)
                if rank then
                  return cjson.encode({status='queued', position=rank})
                end
                
                if redis.call('zscore', 'active_users:'..event_id, user_id) then
                  return cjson.encode({status='active'})
                end
                
                return cjson.encode({status='out'})
                
                """;
        String resultJson = redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, String.class),
                Collections.emptyList(),
                eventId,
                userId.toString()
        );
        log.info("resultJson {}", resultJson);

        return null;
    }

}
