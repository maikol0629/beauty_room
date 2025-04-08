package com.mr.sb.beauty_room.Services;


import com.mr.sb.beauty_room.DTOS.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist.StylistSaveDto;
import com.mr.sb.beauty_room.entities.Stylist;

import java.util.List;
import java.util.Optional;

public interface IStylistService {

    List<StylistResponseDto> findAll();
    StylistResponseDto findById(long id);
    boolean save (StylistSaveDto stylistSaveDto);
    boolean deleteById(long id);
    boolean update (StylistSaveDto stylistSaveDto, long id);
    boolean isExistStylist(long id);
}
