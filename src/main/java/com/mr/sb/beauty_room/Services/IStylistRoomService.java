package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.stylist_room.StylistRoomResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist_room.StylistRoomRequestDto;

import java.util.List;

public interface IStylistRoomService {
    List<StylistRoomResponseDto> findAll();
    StylistRoomResponseDto findById(long id);
    StylistRoomResponseDto save(StylistRoomRequestDto request);
    StylistRoomResponseDto update(long id, StylistRoomRequestDto request);
    boolean deleteById(long id);
}
