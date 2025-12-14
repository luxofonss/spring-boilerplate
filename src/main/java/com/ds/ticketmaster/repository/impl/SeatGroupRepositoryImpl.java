package com.ds.ticketmaster.repository.impl;

import com.ds.ticketmaster.entity.SeatGroup;
import com.ds.ticketmaster.mapper.SeatGroupMapper;
import com.ds.ticketmaster.repository.SeatGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SeatGroupRepositoryImpl implements SeatGroupRepository {

    private final SeatGroupMapper seatGroupMapper;

    @Override
    public int deleteId(String id) {
        return seatGroupMapper.deleteByPrimaryKey(UUID.fromString(id));
    }

    @Override
    public int save(SeatGroup row) {
        return seatGroupMapper.insert(row);
    }

    @Override
    public SeatGroup getById(String id) {
        return seatGroupMapper.selectByPrimaryKey(UUID.fromString(id));
    }

    @Override
    public List<SeatGroup> getAll() {
        return seatGroupMapper.selectAll();
    }

    @Override
    public List<SeatGroup> getByEventId(String eventId) {
        return seatGroupMapper.selectByEventId(UUID.fromString(eventId));
    }

    @Override
    public int updateById(SeatGroup row) {
        return seatGroupMapper.updateByPrimaryKey(row);
    }

    @Override
    public SeatGroup getByIdAndEventId(String seatGroupId, String eventId) {
        return seatGroupMapper.selectByIdAndEventId(UUID.fromString(seatGroupId), UUID.fromString(eventId));
    }
}
