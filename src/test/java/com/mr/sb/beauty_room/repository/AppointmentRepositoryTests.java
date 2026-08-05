package com.mr.sb.beauty_room.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppointmentRepositoryTests {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void existsOverlappingAppointment_shouldReturnFalseWhenNoAppointments() {
        boolean exists = appointmentRepository.existsOverlappingAppointment(
                1L,
                1L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1)
        );

        assertThat(exists).isFalse();
    }
}
