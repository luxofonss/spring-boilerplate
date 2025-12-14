package com.ds.ticketmaster.repository;

import com.ds.ticketmaster.dto.EventDetailDTO;
import com.ds.ticketmaster.entity.Event;

import java.util.List;

public interface EventRepository {

    List<Event> getAllEvents();

    EventDetailDTO getEventDetail(String id);

}
