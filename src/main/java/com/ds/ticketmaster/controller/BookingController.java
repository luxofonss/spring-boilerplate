package com.ds.ticketmaster.controller;

import com.ds.ticketmaster.dto.BaseResponse;
import com.ds.ticketmaster.dto.JoinInQueueRequestDTO;
import com.ds.ticketmaster.dto.JoinInQueueResponseDTO;
import com.ds.ticketmaster.service.BaseService;
import com.ds.ticketmaster.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BaseService baseService;

    private final BookingService bookingService;

    @PostMapping("/queue")
    public BaseResponse<JoinInQueueResponseDTO> jumpInQueue(@RequestBody JoinInQueueRequestDTO request) {
        return baseService.ofSucceeded(bookingService.joinQueue(request));
    }

    @GetMapping("/queue/position")
    public BaseResponse<JoinInQueueResponseDTO> getQueuePosition(@RequestParam String eventId, @RequestParam Long userId) {
        return baseService.ofSucceeded(bookingService.getQueuePosition(eventId, userId));
    }

}
