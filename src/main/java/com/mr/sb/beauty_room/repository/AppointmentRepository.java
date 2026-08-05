package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends CrudRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a WHERE a.stylist.id = :id_stylist AND a.tenant.id = :tenantId")
    List<Appointment> findAppointmentsByStylistId(@Param("id_stylist") long id_stylist, @Param("tenantId") Long tenantId);

    @Query("SELECT a FROM Appointment a WHERE a.client.id = :id_client AND a.tenant.id = :tenantId")
    List<Appointment> findAppointmentsByClientId(@Param("id_client") long id_client, @Param("tenantId") Long tenantId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
            "WHERE a.stylist.id = :stylistId AND a.tenant.id = :tenantId " +
            "AND ( (a.startDate <= :startDate AND a.endDate > :startDate) " +
            "   OR (a.startDate < :endDate AND a.endDate >= :endDate) " +
            "   OR (a.startDate >= :startDate AND a.endDate <= :endDate) )")
    boolean existsOverlappingAppointment(@Param("stylistId") Long stylistId,
                                         @Param("tenantId") Long tenantId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    List<Appointment> findByStylistIdAndStartDateBetweenAndStatusNotAndTenantId(
            Long stylistId, LocalDateTime start, LocalDateTime end, AppointmentStatus status, Long tenantId);

    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND " +
            "(:stylistId IS NULL OR a.stylist.id = :stylistId) AND " +
            "(:clientId IS NULL OR a.client.id = :clientId) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:from IS NULL OR a.startDate >= :from) AND " +
            "(:to IS NULL OR a.startDate <= :to) " +
            "ORDER BY a.startDate DESC")
    List<Appointment> findAppointmentsByFilters(
            @Param("tenantId") Long tenantId,
            @Param("stylistId") Long stylistId,
            @Param("clientId") Long clientId,
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<Appointment> findByTenantId(Long tenantId);

}
