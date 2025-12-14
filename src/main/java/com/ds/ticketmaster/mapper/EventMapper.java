package com.ds.ticketmaster.mapper;

import com.ds.ticketmaster.entity.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface EventMapper {
    int deleteByPrimaryKey(@Param("id") UUID id);

    int insert(Event row);

    Event selectByPrimaryKey(@Param("id") UUID id);

    List<Event> selectAll();

    int updateByPrimaryKey(Event row);
}