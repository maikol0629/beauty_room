package com.mr.sb.beauty_room.dto.stylistroom;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistRoomRequestDto {
    @NotBlank
    private String name;

    @NotBlank
    private String address;
}
