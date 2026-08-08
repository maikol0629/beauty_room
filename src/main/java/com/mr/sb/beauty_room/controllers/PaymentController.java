package com.mr.sb.beauty_room.controllers;

import com.mr.sb.beauty_room.dto.payment.PaymentRequestDto;
import com.mr.sb.beauty_room.dto.payment.PaymentResponseDto;
import com.mr.sb.beauty_room.services.IPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PaymentResponseDto> create(@Valid @RequestBody PaymentRequestDto request) {
        return ResponseEntity.ok(paymentService.createPayment(request));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<PaymentResponseDto> process(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.processPayment(id));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDto> refund(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getById(@PathVariable Long id) {
        PaymentResponseDto payment = paymentService.getPaymentById(id);
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<List<PaymentResponseDto>> getByAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(paymentService.getPaymentsByAppointment(appointmentId));
    }
}
