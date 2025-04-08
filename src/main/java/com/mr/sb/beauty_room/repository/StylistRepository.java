package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.Stylist;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StylistRepository extends JpaRepository<Stylist, Long> {

    @Query("SELECT s FROM Stylist s WHERE s.id  = :id_stylist")
    List<Appointment> findAppointmentsByStylistId(@Param("id_stylist") long id_stylist);

    Optional<Stylist> findByEmail(String email);
}
