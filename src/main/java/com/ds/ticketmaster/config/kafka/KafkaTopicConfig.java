package com.ds.ticketmaster.config.kafka;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class KafkaTopicConfig {

    @Value("${app.topic.random-topic}")
    private String randomKafkaTopic;

}
