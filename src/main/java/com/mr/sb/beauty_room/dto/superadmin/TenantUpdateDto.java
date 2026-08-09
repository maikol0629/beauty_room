package com.mr.sb.beauty_room.dto.superadmin;

import com.mr.sb.beauty_room.entities.TenantPlan;
import com.mr.sb.beauty_room.entities.TenantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantUpdateDto {

    @NotBlank(message = "El nombre del salón es obligatorio")
    private String name;

    @NotNull(message = "El plan es obligatorio")
    private TenantPlan plan;

    @NotNull(message = "El estado es obligatorio")
    private TenantStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime trialEndsAt;
}
