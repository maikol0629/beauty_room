package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Payment;
import com.mr.sb.beauty_room.entities.PaymentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, Long> {
    List<Payment> findByAppointmentIdAndTenantId(Long appointmentId, Long tenantId);

    List<Payment> findByTenantId(Long tenantId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = 'PAID' AND p.paidAt BETWEEN :from AND :to")
    Double totalRevenueBetween(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT p FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = :status")
    List<Payment> findByStatus(@Param("tenantId") Long tenantId, @Param("status") PaymentStatus status);
}
