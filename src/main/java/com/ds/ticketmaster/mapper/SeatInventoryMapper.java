package com.ds.ticketmaster.mapper;

import com.ds.ticketmaster.entity.SeatInventory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.UUID;

@Mapper
public interface SeatInventoryMapper {
    int deleteByPrimaryKey(UUID id);

    int insert(SeatInventory row);

    SeatInventory selectByPrimaryKey(UUID id);

    List<SeatInventory> selectAll();

    int updateByPrimaryKey(SeatInventory row);
}