package com.ds.ticketmaster.dto;

import com.ds.ticketmaster.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventDetailDTO extends Event {

    private List<SeatGroupDTO> seatGroups;

}
