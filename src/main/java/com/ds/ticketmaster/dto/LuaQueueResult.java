package com.ds.ticketmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LuaQueueResult {
    private Boolean alreadyInQueue;
    private Long position;
}