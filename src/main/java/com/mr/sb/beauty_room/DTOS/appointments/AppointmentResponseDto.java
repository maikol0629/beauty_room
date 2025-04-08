package com.mr.sb.beauty_room.DTOS.appointments;

import com.mr.sb.beauty_room.DTOS.client.ClientResponseDto;
import com.mr.sb.beauty_room.DTOS.service.ServiceResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist.StylistResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {

    private long id;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
    private ClientResponseDto client;

    private StylistResponseDto stylist;

    private ServiceResponseDto service;

}
