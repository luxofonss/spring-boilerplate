package com.ds.ticketmaster.controller;

import com.ds.ticketmaster.constant.ErrorConstant;
import com.ds.ticketmaster.dto.BaseResponse;
import com.ds.ticketmaster.entity.Event;
import com.ds.ticketmaster.exception.BusinessException;
import com.ds.ticketmaster.mapper.EventMapper;
import com.ds.ticketmaster.service.BaseService;
import com.ds.ticketmaster.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
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
}
