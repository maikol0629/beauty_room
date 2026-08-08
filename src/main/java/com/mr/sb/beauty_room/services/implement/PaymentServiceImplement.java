package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.payment.PaymentRequestDto;
import com.mr.sb.beauty_room.dto.payment.PaymentResponseDto;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.services.IPaymentService;
import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.Payment;
import com.mr.sb.beauty_room.entities.PaymentStatus;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImplement implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .filter(a -> a.getTenant() != null && a.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada"));

        Payment payment = Payment.builder()
                .appointment(appointment)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .method(request.getMethod())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .tenant(Tenant.builder().id(tenantId).build())
                .build();

        return toDto(paymentRepository.save(payment));
    }

    @Override
    public PaymentResponseDto processPayment(Long paymentId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Payment payment = findForTenant(paymentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("El pago no está pendiente");
        }

        log.info("=== PROCESANDO PAGO {} ===", paymentId);
        log.info("Monto: {} {}", payment.getAmount(), payment.getCurrency());
        log.info("Método: {}", payment.getMethod());

        // Simula procesamiento exitoso
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Pago {} procesado exitosamente. Transacción: {}", paymentId, payment.getTransactionId());

        return toDto(payment);
    }

    @Override
    public PaymentResponseDto refundPayment(Long paymentId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Payment payment = findForTenant(paymentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Solo se pueden reembolsar pagos completados");
        }

        log.info("=== REEMBOLSANDO PAGO {} ===", paymentId);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        log.info("Pago {} reembolsado exitosamente.", paymentId);

        return toDto(payment);
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByAppointment(Long appointmentId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return paymentRepository.findByAppointmentIdAndTenantId(appointmentId, tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public PaymentResponseDto getPaymentById(Long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return findForTenant(id, tenantId)
                .map(this::toDto)
                .orElse(null);
    }

    private Optional<Payment> findForTenant(Long id, Long tenantId) {
        return paymentRepository.findById(id)
                .filter(p -> p.getTenant() != null && p.getTenant().getId().equals(tenantId));
    }

    private PaymentResponseDto toDto(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .appointmentId(payment.getAppointment().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
