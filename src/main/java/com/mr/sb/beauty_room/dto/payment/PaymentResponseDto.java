package com.mr.sb.beauty_room.dto.payment;

import com.mr.sb.beauty_room.entities.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private Long id;
    private Long appointmentId;
    private Double amount;
    private String currency;
    private String method;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
