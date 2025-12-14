package com.ds.ticketmaster.mapper;

import com.ds.ticketmaster.entity.SeatGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface SeatGroupMapper {
    int deleteByPrimaryKey(@Param("id") UUID id);

    int insert(SeatGroup row);

    SeatGroup selectByPrimaryKey(@Param("id") UUID id);

    List<SeatGroup> selectAll();

    List<SeatGroup> selectByEventId(@Param("eventId") UUID eventId);

    int updateByPrimaryKey(SeatGroup row);
}