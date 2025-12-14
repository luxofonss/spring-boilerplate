package com.ds.ticketmaster.dto;

import com.ds.ticketmaster.entity.SeatGroup;
import com.ds.ticketmaster.entity.SeatInventory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeatGroupDTO extends SeatGroup {

    private List<SeatInventory> seatInventories;

}
