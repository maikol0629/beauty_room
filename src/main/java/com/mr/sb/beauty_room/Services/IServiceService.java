package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.service.ServiceResponseDto;
import com.mr.sb.beauty_room.DTOS.service.ServiceSaveDto;
import com.mr.sb.beauty_room.entities.Service;

import java.util.List;
import java.util.Optional;

public interface IServiceService {
    List<ServiceResponseDto> findAll();

    ServiceResponseDto findById(long id);

    List<ServiceResponseDto> findByStylystId(Long id);

    void save(ServiceSaveDto serviceSaveDto);

    boolean deleteById(long id);

    boolean update(ServiceSaveDto serviceSaveDto, long id);
    boolean isExistService(long id);
    boolean serviceBelongToStylyst(long stylystId, long serviceId);
}
