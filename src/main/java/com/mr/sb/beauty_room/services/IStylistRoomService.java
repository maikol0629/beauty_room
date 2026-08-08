package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.stylistroom.StylistRoomResponseDto;
import com.mr.sb.beauty_room.dto.stylistroom.StylistRoomRequestDto;

import java.util.List;

public interface IStylistRoomService {
    List<StylistRoomResponseDto> findAll();
    StylistRoomResponseDto findById(long id);
    StylistRoomResponseDto save(StylistRoomRequestDto request);
    StylistRoomResponseDto update(long id, StylistRoomRequestDto request);
    boolean deleteById(long id);
}
