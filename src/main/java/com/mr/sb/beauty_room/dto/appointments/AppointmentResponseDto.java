package com.mr.sb.beauty_room.dto.appointments;

import com.mr.sb.beauty_room.dto.client.ClientResponseDto;
import com.mr.sb.beauty_room.dto.service.SalonServiceResponseDto;
import com.mr.sb.beauty_room.dto.stylist.StylistResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.mr.sb.beauty_room.entities.AppointmentStatus;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {

    private long id;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
    private AppointmentStatus status;
    private ClientResponseDto client;

    private StylistResponseDto stylist;

    private SalonServiceResponseDto service;

}
