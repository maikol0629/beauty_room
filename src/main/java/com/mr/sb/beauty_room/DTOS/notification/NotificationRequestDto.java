package com.mr.sb.beauty_room.DTOS.notification;

import com.mr.sb.beauty_room.entities.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto {
    @NotNull
    private Long userId;

    @NotNull
    private NotificationType type;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;
}
