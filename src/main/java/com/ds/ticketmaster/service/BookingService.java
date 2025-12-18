package com.ds.ticketmaster.service;

import com.ds.ticketmaster.dto.JoinInQueueRequestDTO;
import com.ds.ticketmaster.dto.JoinInQueueResponseDTO;

public interface BookingService {

    JoinInQueueResponseDTO joinQueue(JoinInQueueRequestDTO request);

    JoinInQueueResponseDTO getQueuePosition(String eventId, Long userId);

}
