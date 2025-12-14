package com.ds.ticketmaster.repository;

import com.ds.ticketmaster.entity.SeatGroup;

import java.util.List;

public interface SeatGroupRepository {

    int deleteId(String id);

    int save(SeatGroup row);

    SeatGroup getById(String id);

    List<SeatGroup> getAll();

    List<SeatGroup> getByEventId(String eventId);

    int updateById(SeatGroup row);

    SeatGroup getByIdAndEventId(String seatGroupId, String eventId);
}