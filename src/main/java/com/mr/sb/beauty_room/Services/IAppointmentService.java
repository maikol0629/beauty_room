package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.DTOS.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.entities.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    boolean confirmAppointment(long id);

    boolean rejectAppointment(long id);

    boolean cancelAppointment(long id);

    boolean completeAppointment(long id);

    List<AppointmentResponseDto> findAppointmentsByFilters(
            Long stylistId, Long clientId, AppointmentStatus status,
            LocalDateTime from, LocalDateTime to);

    List<LocalTime> getAvailableSlots(Long stylistId, Long serviceId, LocalDate date);

}
