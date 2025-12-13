package com.ds.ticketmaster.service.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class EventLogConsumer {

    @KafkaListener(topics = "${app.topic.random-topic}", groupId = "${app.group.ticketmaster-processor-group}", containerFactory = "batchKafkaListenerContainerFactory")
    public void consumeEventLog(java.util.List<Map<String, Object>> messages) {
        try {
            log.info("Received batch of {} event logs", messages.size());
            for (Map<String, Object> message : messages) {
                log.info("Processing event log: {}", message);
            }
        } catch (Exception e) {
            log.error("Error processing batch messages", e);
        }
    }
}
