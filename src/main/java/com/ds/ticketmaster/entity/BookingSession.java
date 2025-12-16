package com.ds.ticketmaster.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingSession {
    private Long id;

    private String token;

    private Long userId;

    private String eventId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
