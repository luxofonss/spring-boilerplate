package com.ds.ticketmaster.service.impl;

import com.ds.ticketmaster.config.kafka.KafkaProducer;
import com.ds.ticketmaster.config.kafka.KafkaTopicConfig;
import com.ds.ticketmaster.constant.ErrorConstant;
import com.ds.ticketmaster.dto.EventDetailDTO;
import com.ds.ticketmaster.dto.ReservationRequestDTO;
import com.ds.ticketmaster.entity.Event;
import com.ds.ticketmaster.entity.SeatGroup;
import com.ds.ticketmaster.exception.BusinessException;
import com.ds.ticketmaster.repository.EventRepository;
import com.ds.ticketmaster.repository.SeatGroupRepository;
import com.ds.ticketmaster.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final KafkaProducer kafkaProducer;

    private final KafkaTopicConfig kafkaTopicConfig;

    private final EventRepository eventRepository;

    private final SeatGroupRepository seatGroupRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        Map<String, Object> eventLog = new java.util.HashMap<>();
        eventLog.put("action", "GET_ALL_EVENTS");
        eventLog.put("timestamp", System.currentTimeMillis());
        kafkaProducer.produce(kafkaTopicConfig.getRandomKafkaTopic(), eventLog);

        return eventRepository.getAllEvents();
    }

    @Override
    public EventDetailDTO getEventDetail(String id) {
        return eventRepository.getEventDetail(id);
    }

    @Override
    public Object processReservationRequest(String eventId, ReservationRequestDTO request) {
        validateReservationRequest(eventId, request);
        return null;
    }

    private void validateReservationRequest(String eventId, ReservationRequestDTO request) {
        Event event = eventRepository.getById(eventId);
        if (event == null) {
            throw new BusinessException(ErrorConstant.NOT_FOUND, "event " + eventId);
        }

        if (!CollectionUtils.isEmpty(request.getSeatGroups())) {
            for (var item : request.getSeatGroups()) {
                SeatGroup group = seatGroupRepository.getByIdAndEventId(item.getSeatGroupId(), eventId);
            }
        }
    }
}
