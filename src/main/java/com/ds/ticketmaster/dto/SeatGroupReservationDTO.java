package com.ds.ticketmaster.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SeatGroupReservationDTO {

    private String seatGroupId;

    private Integer quantity;

}
