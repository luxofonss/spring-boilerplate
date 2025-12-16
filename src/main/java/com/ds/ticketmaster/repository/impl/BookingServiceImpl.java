package com.ds.ticketmaster.repository.impl;

import com.ds.ticketmaster.dto.JoinInQueueRequestDTO;
import com.ds.ticketmaster.dto.JoinInQueueResponseDTO;
import com.ds.ticketmaster.dto.QueueSessionCache;
import com.ds.ticketmaster.entity.BookingSession;
import com.ds.ticketmaster.mapper.BookingSessionMapper;
import com.ds.ticketmaster.service.BookingService;
import com.ds.ticketmaster.service.redis.RedisService;
import com.ds.ticketmaster.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final RedisService redisService;
    private final BookingSessionMapper bookingSessionMapper;
    private final JwtUtils jwtUtils;
    private final Long defaultSessionTTL = 86400L; // 1 day
    private final Long reservationTTL = 600L; // 10 minutes
    private final Integer maxBookingPool = 1000;

    @Qualifier("cacheThreadPool")
    private final Executor executor;

    @Override
    public JoinInQueueResponseDTO joinQueue(JoinInQueueRequestDTO request) {
        String sessionKey = buildJoiningQueueKey(request); // session:{eventId}:{userId}
        String counterKey = buildBookingRoomCountKey(request.getEventId()); // total:ingestion_count:{eventId}

        Map<String, Object> claims = new HashMap<>();
        claims.put("eventId", request.getEventId());

        String token = jwtUtils.generateToken(claims, String.valueOf(request.getUserId()), defaultSessionTTL);

        QueueSessionCache newSessionCache = QueueSessionCache.builder()
                .token(token)
                .status("WAITING")
                .build();
        Long ticketNumber = redisService.executeJoinQueueScript(
                counterKey,
                sessionKey,
                newSessionCache,
                defaultSessionTTL
        );

        if (ticketNumber > 0) {
            log.info("User {} successfully joined the queue. Ticket number: {}", request.getUserId(), ticketNumber);

            saveBookingSession(request, token);

            JoinInQueueResponseDTO response = new JoinInQueueResponseDTO();
            response.setToken(token);
            response.setPosition(calculateCurrentPosition(request.getEventId(), ticketNumber));
            return response;

        } else {
            log.info("User {} re-joined (session exists). Retrieving existing session.", request.getUserId());

            QueueSessionCache existingSession = redisService.get(sessionKey, QueueSessionCache.class);

            JoinInQueueResponseDTO response = new JoinInQueueResponseDTO();

            if (existingSession != null) {
                Long existingTicketNumber = existingSession.getPosition() != null ? existingSession.getPosition() : 0L;

                response.setToken(existingSession.getToken());
                response.setPosition(calculateCurrentPosition(request.getEventId(), existingTicketNumber));
                log.info("User {} re-joined. Ticket number: {}, Current Pos: {}", request.getUserId(), existingTicketNumber, response.getPosition());
            } else {
                // key deleted after script run. break consistency.
                // Rarely happen -> TODO
                log.error("CONSISTENCY ERROR: User {}'s session was expected to exist but was NULL after re-join attempt.", request.getUserId());
                response.setToken(token);
                response.setPosition(-1);
            }
            return response;
        }
    }
    private void saveBookingSession(JoinInQueueRequestDTO request, String token) {
        CompletableFuture.runAsync(() -> {
            try {
                BookingSession session = BookingSession.builder()
                        .token(token)
                        .userId(request.getUserId())
                        .eventId(request.getEventId())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                bookingSessionMapper.insert(session);
            } catch (Exception e) {
                log.error("Error saving booking session for user {} and event {}", request.getUserId(), request.getEventId(), e);
            }
        }, executor);
    }

    private Integer calculateCurrentPosition(String eventId, Long ticketNumber) {
        String processedKey = buildProcessedCountKey(eventId);
        Long processedCount = redisService.get(processedKey, Long.class);
        if (processedCount == null) {
            processedCount = 0L;
        }
        long currentPosition = ticketNumber - processedCount;
        return Math.toIntExact(Math.max(currentPosition, 0));
    }

    private String buildBookingRoomCountKey(String eventId) {
        return String.format("total:ingestion_count:%s", eventId);
    }

    private String buildProcessedCountKey(String eventId) {
        return String.format("total:processed_count:%s", eventId);
    }

    private String buildJoiningQueueKey(JoinInQueueRequestDTO request) {
        return String.format("session:%s:%s", request.getEventId(), request.getUserId());
    }
}
