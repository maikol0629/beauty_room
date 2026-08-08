package com.mr.sb.beauty_room.dto.appointments;
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
    private Long clientId;

    @NotNull
    private Long stylistId;

    @NotNull
    private Long serviceId;

}
