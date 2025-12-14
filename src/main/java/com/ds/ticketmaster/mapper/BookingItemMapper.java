package com.ds.ticketmaster.mapper;

import com.ds.ticketmaster.entity.BookingItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface BookingItemMapper {
    int deleteByPrimaryKey(@Param("id") UUID id);

    int insert(BookingItem row);

    BookingItem selectByPrimaryKey(@Param("id") UUID id);

    List<BookingItem> selectAll();

    int updateByPrimaryKey(BookingItem row);
}