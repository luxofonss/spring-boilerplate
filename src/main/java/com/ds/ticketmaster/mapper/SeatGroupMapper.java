package com.ds.ticketmaster.mapper;

import com.ds.ticketmaster.entity.SeatGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.UUID;

@Mapper
public interface SeatGroupMapper {
    int deleteByPrimaryKey(UUID id);

    int insert(SeatGroup row);

    SeatGroup selectByPrimaryKey(UUID id);

    List<SeatGroup> selectAll();

    int updateByPrimaryKey(SeatGroup row);
}