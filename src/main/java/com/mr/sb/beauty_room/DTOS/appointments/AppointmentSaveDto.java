package com.mr.sb.beauty_room.DTOS.appointments;

import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Service;
import com.mr.sb.beauty_room.entities.Stylist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSaveDto {

    private LocalDateTime startDate;

    private long id_client;

    private long id_stylist;

    private long id_service;


}
