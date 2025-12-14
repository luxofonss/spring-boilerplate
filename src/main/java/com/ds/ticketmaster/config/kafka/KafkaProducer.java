package com.ds.ticketmaster.config.kafka;

import com.ds.ticketmaster.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void produce(String topic, Object message) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, message);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Send message {} with offset {}", message, result.getRecordMetadata().offset());
            } else {
                log.warn("Send message {} fail due to {}", message, ex.getMessage());
            }
        });
    }

}
