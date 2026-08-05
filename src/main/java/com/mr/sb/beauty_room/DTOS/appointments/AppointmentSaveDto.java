package com.mr.sb.beauty_room.DTOS.appointments;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSaveDto {

    @NotNull
    @Future
    private LocalDateTime startDate;

    @NotNull
    private Long id_client;

    @NotNull
    private Long id_stylist;

    @NotNull
    private Long id_service;

}
