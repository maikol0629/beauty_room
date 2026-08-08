package com.mr.sb.beauty_room.services;
import com.mr.sb.beauty_room.dto.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.dto.stylist.StylistSaveDto;

import java.util.List;

public interface IStylistService {

    List<StylistResponseDto> findAll();
    StylistResponseDto findById(long id);
    boolean save (StylistSaveDto stylistSaveDto);
    boolean deleteById(long id);
    boolean update (StylistSaveDto stylistSaveDto, long id);
    boolean isExistStylist(long id);
}
