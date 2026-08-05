package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.DTOS.payment.PaymentRequestDto;
import com.mr.sb.beauty_room.DTOS.payment.PaymentResponseDto;

import java.util.List;

public interface IPaymentService {
    PaymentResponseDto createPayment(PaymentRequestDto request);
    PaymentResponseDto processPayment(Long paymentId);
    PaymentResponseDto refundPayment(Long paymentId);
    List<PaymentResponseDto> getPaymentsByAppointment(Long appointmentId);
    PaymentResponseDto getPaymentById(Long id);
}
