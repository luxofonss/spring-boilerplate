package com.ds.ticketmaster.repository.impl;

import com.ds.ticketmaster.constant.ErrorConstant;
import com.ds.ticketmaster.dto.EventDetailDTO;
import com.ds.ticketmaster.dto.SeatGroupDTO;
import com.ds.ticketmaster.entity.Event;
import com.ds.ticketmaster.entity.SeatGroup;
import com.ds.ticketmaster.entity.SeatInventory;
import com.ds.ticketmaster.exception.BusinessException;
import com.ds.ticketmaster.mapper.EventMapper;
import com.ds.ticketmaster.mapper.SeatGroupMapper;
import com.ds.ticketmaster.mapper.SeatInventoryMapper;
import com.ds.ticketmaster.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepository {

    private final EventMapper eventMapper;
    private final SeatGroupMapper seatGroupMapper;
    private final SeatInventoryMapper seatInventoryMapper;

    @Override
    public List<Event> getAllEvents() {
        return eventMapper.selectAll();
    }

    @Override
    public EventDetailDTO getEventDetail(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            Event event = eventMapper.selectByPrimaryKey(uuid);
            if (event == null) {
                throw new BusinessException(ErrorConstant.NOT_FOUND);
            }

            List<SeatGroup> seatGroups = seatGroupMapper.selectByEventId(uuid);
            List<SeatGroupDTO> seatGroupDTOs = seatGroups.stream().map(group -> {
                SeatGroupDTO groupDTO = new SeatGroupDTO();
                BeanUtils.copyProperties(group, groupDTO);
                List<SeatInventory> inventories = seatInventoryMapper.selectBySeatGroupId(group.getId());
                groupDTO.setSeatInventories(inventories);
                return groupDTO;
            }).collect(Collectors.toList());
            EventDetailDTO dto = new EventDetailDTO();
            BeanUtils.copyProperties(event, dto);
            dto.setSeatGroups(seatGroupDTOs);
            return dto;
        } catch (Exception e) {
            throw new BusinessException(ErrorConstant.NOT_FOUND);
        }
    }

    @Override
    public Event getById(String eventId) {
        return eventMapper.selectByPrimaryKey(UUID.fromString(eventId));
    }
}
