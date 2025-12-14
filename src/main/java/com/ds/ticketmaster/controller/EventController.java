package com.ds.ticketmaster.controller;

import com.ds.ticketmaster.dto.BaseResponse;
import com.ds.ticketmaster.dto.EventDetailDTO;
import com.ds.ticketmaster.entity.Event;
import com.ds.ticketmaster.service.BaseService;
import com.ds.ticketmaster.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final BaseService baseService;

    private final EventService eventService;

    @GetMapping
    public BaseResponse<List<Event>> getAllEvents() {
        return baseService.ofSucceeded(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public BaseResponse<EventDetailDTO>getEventDetail(@PathVariable String id) {
        return baseService.ofSucceeded(eventService.getEventDetail(id));
    }
}
