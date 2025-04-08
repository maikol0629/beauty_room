package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Appointment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface AppointmentRepository extends CrudRepository<Appointment, Long> {


    @Query("SELECT a FROM Appointment a WHERE a.stylist.id  = :id_stylist")
    List<Appointment> findAppointmentsByStylistId(@Param("id_stylist") long id_stylist);

    @Query("SELECT s FROM Appointment s WHERE s.client.id  = :id_client")
    List<Appointment> findAppointmentsByClientId(@Param("id_client") long id_client);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
            "WHERE a.stylist.id = :stylistId " +
            "AND ( (a.startDate <= :startDate AND a.endDate > :startDate) " +
            "   OR (a.startDate < :endDate AND a.endDate >= :endDate) " +
            "   OR (a.startDate >= :startDate AND a.endDate <= :endDate) )")
    boolean existsOverlappingAppointment(@Param("stylistId") Long stylistId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

}
