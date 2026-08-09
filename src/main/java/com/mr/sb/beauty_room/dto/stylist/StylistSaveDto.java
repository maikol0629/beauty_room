package com.mr.sb.beauty_room.dto.stylist;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistSaveDto {
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Email
    private String email;

    /**
     * Contraseña del estilista. Obligatoria al crear; se ignora en edición
     * (no se valida aquí porque el form de edición la deja vacía).
     */
    private String password;

    @NotBlank
    @Size(min = 7, max = 20)
    private String phone;
    private long stylistRoomId;
    private String telegramChatId;
}
