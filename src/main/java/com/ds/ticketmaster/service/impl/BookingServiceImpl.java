package com.ds.ticketmaster.service.impl;

import com.ds.ticketmaster.config.cache.RedisScriptManager;
import com.ds.ticketmaster.config.kafka.KafkaProducer;
import com.ds.ticketmaster.constant.Constant;
import com.ds.ticketmaster.constant.ErrorConstant;
import com.ds.ticketmaster.dto.*;
import com.ds.ticketmaster.exception.BusinessException;
import com.ds.ticketmaster.service.BookingService;
import com.ds.ticketmaster.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final KafkaProducer kafkaProducer;

    private final RedisScriptManager redisScriptManager;

    public BookingServiceImpl(@Qualifier("redisTemplateWithStringKey") RedisTemplate<String, Object> redisTemplate,
                              KafkaProducer kafkaProducer,
                              RedisScriptManager redisScriptManager) {
        this.redisTemplate = redisTemplate;
        this.kafkaProducer = kafkaProducer;
        this.redisScriptManager = redisScriptManager;
    }


    @Override
    public JoinInQueueResponseDTO joinQueue(JoinInQueueRequestDTO request) {
        RedisScript<String> script = redisScriptManager.getScript(Constant.LuaScript.JOIN_QUEUE_SCRIPT);

        String resultStr = redisTemplate.execute(
                script,
                Collections.emptyList(),
                request.getEventId(),
                request.getUserId().toString()
        );
        LuaQueueResult result = JsonUtils.getEntityFromJsonStr(resultStr, LuaQueueResult.class);
        if (result == null) {
            throw new BusinessException(ErrorConstant.INTERNAL_SERVER_ERROR);
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
        RedisScript<String> script = redisScriptManager.getScript(Constant.LuaScript.GET_QUEUE_POSITION);
        String resultStr = redisTemplate.execute(
                script,
                Collections.emptyList(),
                eventId,
                userId.toString()
        );
        LuaQueuePositionResult result = JsonUtils.getEntityFromJsonStr(resultStr, LuaQueuePositionResult.class);
        if (result == null) {
            throw new BusinessException(ErrorConstant.INTERNAL_SERVER_ERROR);
        }
        log.info("resultJson {}", resultStr);
        // TODO: handle logic to calculate return status

        return JoinInQueueResponseDTO.builder().build();
    }

}
