package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.DTOS.appointments.AppointmentSaveDto;

import java.util.List;
import java.util.Optional;

public interface IAppointmentService {

    List<AppointmentResponseDto> findAll();

    Optional<AppointmentResponseDto> findById(long id);

    List<AppointmentResponseDto> findAppointmentsByStylistID(long id);

    List<AppointmentResponseDto> findAppointmentsByClientID(long id);

    boolean save(AppointmentSaveDto appointmentSaveDto);

    boolean update(AppointmentSaveDto appointmentSaveDto, long id);

    boolean deleteById(long id);


}
