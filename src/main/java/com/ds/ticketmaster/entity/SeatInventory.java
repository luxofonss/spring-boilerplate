package com.ds.ticketmaster.entity;

import com.ds.ticketmaster.type.SeatInventoryStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatInventory {
    private UUID id;

    private UUID eventId;

    private UUID seatTypeId;

    private String seatNum;

    private SeatInventoryStatus status;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", eventId=").append(eventId);
        sb.append(", seatTypeId=").append(seatTypeId);
        sb.append(", seatNum=").append(seatNum);
        sb.append(", status=").append(status);
        sb.append(", version=").append(version);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append("]");
        return sb.toString();
    }
}