package com.ds.ticketmaster.config.kafka;

import com.ds.ticketmaster.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void produce(String topic, Object object) {
        kafkaTemplate.send(topic, object);
    }

}
