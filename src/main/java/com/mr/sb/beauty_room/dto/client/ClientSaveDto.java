package com.mr.sb.beauty_room.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientSaveDto {
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 7, max = 20)
    private String phone;

    private String telegramChatId;
}
