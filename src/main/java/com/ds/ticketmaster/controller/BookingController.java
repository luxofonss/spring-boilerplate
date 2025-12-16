package com.ds.ticketmaster.controller;

import com.ds.ticketmaster.dto.BaseResponse;
import com.ds.ticketmaster.dto.JoinInQueueRequestDTO;
import com.ds.ticketmaster.service.BaseService;
import com.ds.ticketmaster.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BaseService baseService;

    private final BookingService bookingService;

    @PostMapping("/queue")
    public BaseResponse<Object> jumpInQueue(@RequestBody JoinInQueueRequestDTO request) {
        return baseService.ofSucceeded(bookingService.joinQueue(request));
    }

}
