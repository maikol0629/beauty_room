package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.StylistSchedule;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.SalonServiceRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.StylistScheduleRepository;
import com.mr.sb.beauty_room.services.IBlockedSlotService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.mr.sb.beauty_room.services.INotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplementTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private StylistRepository stylistRepository;
    @Mock
    private SalonServiceRepository serviceRepository;
    @Mock
    private StylistScheduleRepository stylistScheduleRepository;
    @Mock
    private IBlockedSlotService blockedSlotService;
    @Mock
    private INotificationService notificationService;
    @Mock
    private IMessagingChannel messagingChannel;

    @InjectMocks
    private AppointmentServiceImplement appointmentService;

    private Client client;
    private Stylist stylist;
    private SalonService service;

    @BeforeEach
    void setUp() {
        TenantInterceptor.setCurrentTenantId(TENANT_ID);

        client = new Client();
        client.setId(1L);
        client.setNameClient("Juan Perez");
        client.setPhone("3000000000");

        stylist = new Stylist();
        stylist.setId(2L);
        stylist.setNameStylist("Maria");
        stylist.setPhone("3100000000");

        service = SalonService.builder()
                .id(3L)
                .nameService("Corte")
                .description("Corte basico")
                .price(15000)
                .duration(30)
                .stylist(stylist)
                .build();

        stylist.getServices().add(service);
    }

    @AfterEach
    void tearDown() {
        TenantInterceptor.clear();
    }

    @Test
    void save_shouldCreateAppointmentWhenDataIsValid() {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

        AppointmentSaveDto dto = AppointmentSaveDto.builder()
                .startDate(start)
                .clientId(client.getId())
                .stylistId(stylist.getId())
                .serviceId(service.getId())
                .build();

        StylistSchedule schedule = StylistSchedule.builder()
                .id(1L)
                .stylist(stylist)
                .day(start.getDayOfWeek())
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(22, 0))
                .build();

        when(clientRepository.findByIdAndTenantId(client.getId(), TENANT_ID)).thenReturn(Optional.of(client));
        when(stylistRepository.findByIdAndTenantId(stylist.getId(), TENANT_ID)).thenReturn(Optional.of(stylist));
        when(serviceRepository.findByIdAndTenantId(service.getId(), TENANT_ID)).thenReturn(Optional.of(service));
        when(stylistScheduleRepository.findByStylistIdAndDayAndTenantId(stylist.getId(), start.getDayOfWeek(), TENANT_ID))
                .thenReturn(List.of(schedule));
        when(appointmentRepository.existsOverlappingAppointment(eq(stylist.getId()), eq(TENANT_ID), eq(start), any()))
                .thenReturn(false);
        when(blockedSlotService.isSlotBlocked(eq(TENANT_ID), anyLong(), any(), any())).thenReturn(false);

        boolean result = appointmentService.save(dto);

        assertThat(result).isTrue();

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        Appointment saved = captor.getValue();
        assertThat(saved.getClient()).isEqualTo(client);
        assertThat(saved.getStylist()).isEqualTo(stylist);
        assertThat(saved.getService()).isEqualTo(service);
        assertThat(saved.getStartDate()).isEqualTo(start);
        assertThat(saved.getEndDate()).isEqualTo(start.plusMinutes(service.getDuration()));
    }

    @Test
    void save_shouldReturnFalseWhenServiceDoesNotBelongToStylist() {
        // stylist sin servicios asociados
        stylist.getServices().clear();

        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

        AppointmentSaveDto dto = AppointmentSaveDto.builder()
                .startDate(start)
                .clientId(client.getId())
                .stylistId(stylist.getId())
                .serviceId(service.getId())
                .build();

        when(clientRepository.findByIdAndTenantId(client.getId(), TENANT_ID)).thenReturn(Optional.of(client));
        when(stylistRepository.findByIdAndTenantId(stylist.getId(), TENANT_ID)).thenReturn(Optional.of(stylist));
        when(serviceRepository.findByIdAndTenantId(service.getId(), TENANT_ID)).thenReturn(Optional.of(service));

        boolean result = appointmentService.save(dto);

        assertThat(result).isFalse();
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void save_shouldNotifyStylistWhenStylistHasTelegramChatId() {
        stylist.setTelegramChatId("555000111");

        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

        AppointmentSaveDto dto = AppointmentSaveDto.builder()
                .startDate(start)
                .clientId(client.getId())
                .stylistId(stylist.getId())
                .serviceId(service.getId())
                .build();

        StylistSchedule schedule = StylistSchedule.builder()
                .id(1L)
                .stylist(stylist)
                .day(start.getDayOfWeek())
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(22, 0))
                .build();

        when(clientRepository.findByIdAndTenantId(client.getId(), TENANT_ID)).thenReturn(Optional.of(client));
        when(stylistRepository.findByIdAndTenantId(stylist.getId(), TENANT_ID)).thenReturn(Optional.of(stylist));
        when(serviceRepository.findByIdAndTenantId(service.getId(), TENANT_ID)).thenReturn(Optional.of(service));
        when(stylistScheduleRepository.findByStylistIdAndDayAndTenantId(stylist.getId(), start.getDayOfWeek(), TENANT_ID))
                .thenReturn(List.of(schedule));
        when(appointmentRepository.existsOverlappingAppointment(eq(stylist.getId()), eq(TENANT_ID), eq(start), any()))
                .thenReturn(false);
        when(blockedSlotService.isSlotBlocked(eq(TENANT_ID), anyLong(), any(), any())).thenReturn(false);

        boolean result = appointmentService.save(dto);

        assertThat(result).isTrue();
        verify(messagingChannel).sendMessage(eq("555000111"), contains("Nueva cita"));
    }

    @Test
    void save_shouldNotNotifyStylistWithoutTelegramChatId() {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

        AppointmentSaveDto dto = AppointmentSaveDto.builder()
                .startDate(start)
                .clientId(client.getId())
                .stylistId(stylist.getId())
                .serviceId(service.getId())
                .build();

        StylistSchedule schedule = StylistSchedule.builder()
                .id(1L)
                .stylist(stylist)
                .day(start.getDayOfWeek())
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(22, 0))
                .build();

        when(clientRepository.findByIdAndTenantId(client.getId(), TENANT_ID)).thenReturn(Optional.of(client));
        when(stylistRepository.findByIdAndTenantId(stylist.getId(), TENANT_ID)).thenReturn(Optional.of(stylist));
        when(serviceRepository.findByIdAndTenantId(service.getId(), TENANT_ID)).thenReturn(Optional.of(service));
        when(stylistScheduleRepository.findByStylistIdAndDayAndTenantId(stylist.getId(), start.getDayOfWeek(), TENANT_ID))
                .thenReturn(List.of(schedule));
        when(appointmentRepository.existsOverlappingAppointment(eq(stylist.getId()), eq(TENANT_ID), eq(start), any()))
                .thenReturn(false);
        when(blockedSlotService.isSlotBlocked(eq(TENANT_ID), anyLong(), any(), any())).thenReturn(false);

        boolean result = appointmentService.save(dto);

        assertThat(result).isTrue();
        verify(messagingChannel, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void completeAppointment_shouldAcceptPendingOrConfirmed() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.PENDING)
                .tenant(Tenant.builder().id(TENANT_ID).build())
                .build();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appt));

        boolean result = appointmentService.completeAppointment(1L);

        assertThat(result).isTrue();
        verify(appointmentRepository).save(argThat(a -> a.getStatus() == AppointmentStatus.COMPLETED));
    }

    @Test
    void noShowAppointment_shouldSetStatusNoShow() {
        Appointment appt = Appointment.builder()
                .id(2L)
                .status(AppointmentStatus.CONFIRMED)
                .tenant(Tenant.builder().id(TENANT_ID).build())
                .build();
        when(appointmentRepository.findById(2L)).thenReturn(Optional.of(appt));

        boolean result = appointmentService.noShowAppointment(2L);

        assertThat(result).isTrue();
        verify(appointmentRepository).save(argThat(a -> a.getStatus() == AppointmentStatus.NO_SHOW));
    }

    @Test
    void cancelAppointmentByStylist_shouldNotifyClientWhenHasTelegramChatId() {
        client.setTelegramChatId("999000999");
        Appointment appt = Appointment.builder()
                .id(3L)
                .status(AppointmentStatus.CONFIRMED)
                .tenant(Tenant.builder().id(TENANT_ID).build())
                .client(client)
                .service(service)
                .startDate(LocalDateTime.of(2026, 8, 10, 10, 0))
                .build();
        when(appointmentRepository.findById(3L)).thenReturn(Optional.of(appt));

        boolean result = appointmentService.cancelAppointmentByStylist(3L);

        assertThat(result).isTrue();
        verify(appointmentRepository).save(argThat(a -> a.getStatus() == AppointmentStatus.CANCELLED));
        verify(messagingChannel).sendMessage(eq("999000999"), contains("fue cancelada"));
    }
}
