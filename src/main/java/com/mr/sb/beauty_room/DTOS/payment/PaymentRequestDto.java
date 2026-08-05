package com.mr.sb.beauty_room.DTOS.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    @NotNull
    private Long appointmentId;

    @NotNull
    @Positive
    private Double amount;

    private String currency;

    private String method;
}
