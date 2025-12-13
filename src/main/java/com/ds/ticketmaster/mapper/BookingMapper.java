package com.ds.ticketmaster.mapper;

import com.ds.ticketmaster.entity.Booking;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.UUID;

@Mapper
public interface BookingMapper {
    int deleteByPrimaryKey(UUID id);

    int insert(Booking row);

    Booking selectByPrimaryKey(UUID id);

    List<Booking> selectAll();

    int updateByPrimaryKey(Booking row);
}