package com.ds.ticketmaster.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRequestDTO {

    private String userId; // for testing

    private List<String> seatNums;

    private List<SeatGroupReservationDTO> seatGroups;

}
