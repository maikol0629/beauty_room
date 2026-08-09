package com.mr.sb.beauty_room.dto.superadmin;

import com.mr.sb.beauty_room.entities.TenantPlan;
import com.mr.sb.beauty_room.entities.TenantStatus;
import jakarta.validation.constraints.Email;
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
public class TenantCreateDto {

    @NotBlank(message = "El nombre del salón es obligatorio")
    private String name;

    private String tenantKey;

    @NotNull(message = "El plan es obligatorio")
    private TenantPlan plan;

    private TenantStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime trialEndsAt;

    @NotBlank(message = "El email del administrador es obligatorio")
    @Email(message = "Email inválido")
    private String adminEmail;

    @NotBlank(message = "La contraseña del administrador es obligatoria")
    private String adminPassword;

    /** Nombre del administrador. El admin del salón también es estilista. */
    @NotBlank(message = "El nombre del administrador es obligatorio")
    private String adminName;

    private String adminPhone;
}
