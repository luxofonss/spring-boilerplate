package com.ds.ticketmaster.mapper;

import com.ds.ticketmaster.entity.SeatInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface SeatInventoryMapper {
    int deleteByPrimaryKey(@Param("id") UUID id);

    int insert(SeatInventory row);

    SeatInventory selectByPrimaryKey(@Param("id") UUID id);

    List<SeatInventory> selectAll();

    List<SeatInventory> selectBySeatGroupId(@Param("seatGroupId") UUID seatGroupId);

    int updateByPrimaryKey(SeatInventory row);
}