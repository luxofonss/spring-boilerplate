package com.ds.ticketmaster.service;

import com.ds.ticketmaster.dto.EventDetailDTO;
import com.ds.ticketmaster.dto.ReservationRequestDTO;
import com.ds.ticketmaster.entity.Event;

import java.util.List;

public interface EventService {
    List<Event> getAllEvents();

    EventDetailDTO getEventDetail(String id);

    Object processReservationRequest(String eventId, ReservationRequestDTO request);
}
