package com.ds.ticketmaster.service.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class PaymentConsumer {

    @KafkaListener(topics = "${app.topic.payment-topic}", groupId = "${app.group.payment-processor-group}", containerFactory = "strictKafkaListenerContainerFactory")
    public void consumePayment(Map<String, Object> message, Acknowledgment acknowledgment) {
        try {
            log.info("Received payment request: {}", message);

            processPayment(message);

            acknowledgment.acknowledge();
            log.info("Payment processed and offset committed.");
        } catch (Exception e) {
            log.error("Error processing payment. Offset NOT committed. Message will be redelivered.", e);
        }
    }

    private void processPayment(Map<String, Object> message) {
        if (message.containsKey("error")) {
            throw new RuntimeException("Simulated payment failure");
        }
    }
}
