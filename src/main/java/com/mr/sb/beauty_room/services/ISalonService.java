package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.service.SalonServiceResponseDto;
import com.mr.sb.beauty_room.dto.service.SalonServiceSaveDto;

import java.util.List;

public interface ISalonService {
    List<SalonServiceResponseDto> findAll();

    SalonServiceResponseDto findById(long id);

    List<SalonServiceResponseDto> findByStylistId(Long id);

    void save(SalonServiceSaveDto serviceSaveDto);

    boolean deleteById(long id);

    boolean update(SalonServiceSaveDto serviceSaveDto, long id);
    boolean isExistService(long id);
    boolean serviceBelongsToStylist(long stylistId, long serviceId);
}
