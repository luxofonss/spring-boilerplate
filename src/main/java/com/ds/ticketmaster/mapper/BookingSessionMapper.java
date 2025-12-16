package com.ds.ticketmaster.mapper;

import com.ds.ticketmaster.entity.BookingSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookingSessionMapper {
    int insert(BookingSession record);
}
