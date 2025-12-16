package com.ds.ticketmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JoinInQueueResponseDTO {

    private Integer position;

    private String token;

}
