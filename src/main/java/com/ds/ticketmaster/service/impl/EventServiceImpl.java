package com.ds.ticketmaster.service.impl;

import com.ds.ticketmaster.config.kafka.KafkaProducer;
import com.ds.ticketmaster.config.kafka.KafkaTopicConfig;
import com.ds.ticketmaster.dto.EventDetailDTO;
import com.ds.ticketmaster.entity.Event;
import com.ds.ticketmaster.mapper.EventMapper;
import com.ds.ticketmaster.repository.EventRepository;
import com.ds.ticketmaster.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final KafkaProducer kafkaProducer;
    private final KafkaTopicConfig kafkaTopicConfig;
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        Map<String, Object> eventLog = new java.util.HashMap<>();
        eventLog.put("action", "GET_ALL_EVENTS");
        eventLog.put("timestamp", System.currentTimeMillis());
        kafkaProducer.produce(kafkaTopicConfig.getRandomKafkaTopic(), eventLog);

        return eventMapper.selectAll();
    }

    @Override
    public EventDetailDTO getEventDetail(String id) {
        return eventRepository.getEventDetail(id);
    }
}
