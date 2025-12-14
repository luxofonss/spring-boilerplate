package com.ds.ticketmaster.service.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StatisticService {

    @RetryableTopic(attempts = "5", dltTopicSuffix = "-dlt", backoff = @Backoff(delay = 2000, multiplier = 2))
    @KafkaListener(topics = "statistic",groupId = "${app.group.ticketmaster-processor-group}",  containerFactory = "commonKafkaListenerContainerFactory")
    public void listen(String message) {
        log.info("Received: {}", message);
        throw new RuntimeException();
    }

    @KafkaListener(topics = "statistic-dlt",groupId = "${app.group.ticketmaster-processor-group}",  containerFactory = "commonKafkaListenerContainerFactory")
    public void dltListen(String message) {
        log.info("Receive statistic.DLT: {}", message);
    }

}
