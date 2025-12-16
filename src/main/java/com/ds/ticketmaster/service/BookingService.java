package com.ds.ticketmaster.service;

import com.ds.ticketmaster.dto.JoinInQueueRequestDTO;

public interface BookingService {

    Object joinQueue(JoinInQueueRequestDTO request);

}
