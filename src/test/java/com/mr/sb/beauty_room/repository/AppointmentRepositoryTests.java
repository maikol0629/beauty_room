package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Service;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppointmentRepositoryTests {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private StylistRepository stylistRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private TenantRepository tenantRepository;

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

    @Test
    void findRemindable_shouldReturnActiveAppointmentsInWindowWithClientChat() {
        Appointment saved = saveAppointment(LocalDateTime.now().plusHours(3), AppointmentStatus.CONFIRMED, 3L);

        List<Appointment> result = appointmentRepository.findRemindable(
                1L,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(24));

        assertThat(result).anyMatch(a -> a.getId() == saved.getId());
    }

    @Test
    void findRemindable_shouldExcludeCancelledAppointmentsAndClientsWithoutChat() {
        Tenant tenant = tenantRepository.findById(1L).orElseThrow();
        Client noChat = Client.builder()
                .email("tg_nochat@bot.local")
                .password("x")
                .name_client("No Chat")
                .telegram_chat_id(null)
                .tenant(tenant)
                .build();
        noChat = clientRepository.save(noChat);

        LocalDateTime start = LocalDateTime.now().plusHours(5);
        saveAppointmentWithClient(noChat, start, AppointmentStatus.CONFIRMED);
        saveAppointmentWithClient(clientRepository.findByIdAndTenantId(3L, 1L).orElseThrow(), start, AppointmentStatus.CANCELLED);

        List<Appointment> result = appointmentRepository.findRemindable(
                1L,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(24));

        assertThat(result).isEmpty();
    }

    @Test
    void findStylistDay_shouldReturnTodayActiveAppointmentsExcludingCancelled() {
        Appointment active = saveAppointment(LocalDateTime.now().withHour(10).withMinute(0), AppointmentStatus.CONFIRMED, 3L);
        saveAppointment(LocalDateTime.now().withHour(11).withMinute(0), AppointmentStatus.CANCELLED, 3L);

        List<Appointment> day = appointmentRepository.findStylistDay(
                1L,
                1L,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(LocalTime.MAX));

        assertThat(day).anyMatch(a -> a.getId() == active.getId());
        assertThat(day).allMatch(a -> a.getStatus() != AppointmentStatus.CANCELLED
                && a.getStatus() != AppointmentStatus.REJECTED);
    }

    private Appointment saveAppointment(LocalDateTime start, AppointmentStatus status, Long clientId) {
        return saveAppointmentWithClient(
                clientRepository.findByIdAndTenantId(clientId, 1L).orElseThrow(), start, status);
    }

    private Appointment saveAppointmentWithClient(Client client, LocalDateTime start, AppointmentStatus status) {
        Stylist stylist = stylistRepository.findByIdAndTenantId(1L, 1L).orElseThrow();
        Service service = serviceRepository.findByIdAndTenantId(1L, 1L).orElseThrow();
        Tenant tenant = tenantRepository.findById(1L).orElseThrow();
        Appointment appointment = Appointment.builder()
                .startDate(start)
                .endDate(start.plusMinutes(30))
                .client(client)
                .stylist(stylist)
                .service(service)
                .status(status)
                .tenant(tenant)
                .build();
        return appointmentRepository.save(appointment);
    }
}
