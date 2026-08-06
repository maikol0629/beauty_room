package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.Services.IMessagingChannel;
import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.NotificationType;
import com.mr.sb.beauty_room.entities.Service;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.NotificationRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceImplementTest {

    private static final Long TENANT_ID = 1L;
    private static final String CLIENT_CHAT = "999000999";
    private static final String STYLIST_CHAT = "555000111";

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private StylistRepository stylistRepository;
    @Mock
    private IMessagingChannel messagingChannel;

    @InjectMocks
    private ReminderServiceImplement reminderService;

    private Tenant tenant(long id) {
        return Tenant.builder().id(id).build();
    }

    private Appointment appointment(long id, LocalDateTime start, String clientChat) {
        Client client = Client.builder().id(10L).telegram_chat_id(clientChat).build();
        Service service = Service.builder().id(1L).name_service("Haircut").build();
        return Appointment.builder()
                .id(id)
                .startDate(start)
                .status(AppointmentStatus.CONFIRMED)
                .client(client)
                .service(service)
                .tenant(tenant(TENANT_ID))
                .build();
    }

    private void stubCandidates(Appointment appointment) {
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(TENANT_ID)));
        when(appointmentRepository.findRemindable(eq(TENANT_ID), anyList(), any(), any()))
                .thenReturn(List.of(appointment));
    }

    @Test
    void sendUpcomingReminders_appointmentIn24hWindow_shouldSend24hReminderAndMarkSent() {
        Appointment appt = appointment(1L, LocalDateTime.now().plusHours(12), CLIENT_CHAT);
        stubCandidates(appt);
        when(notificationRepository.existsByAppointmentIdAndType(1L, NotificationType.REMINDER_24H)).thenReturn(false);

        reminderService.sendUpcomingReminders();

        verify(messagingChannel).sendInlineKeyboard(eq(CLIENT_CHAT), contains("Recordatorio"),
                argThat(buttons -> buttons.stream()
                        .anyMatch(b -> b.callbackData().equals(ReminderServiceImplement.PREFIX_REMINDER_CONFIRM + "1"))
                        && buttons.stream()
                        .anyMatch(b -> b.callbackData().equals(ReminderServiceImplement.PREFIX_REMINDER_CANCEL + "1"))));
        ArgumentCaptor<com.mr.sb.beauty_room.entities.Notification> captor = ArgumentCaptor.forClass(com.mr.sb.beauty_room.entities.Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.REMINDER_24H);
        assertThat(captor.getValue().getAppointmentId()).isEqualTo(1L);
        assertThat(captor.getValue().getUserId()).isEqualTo(10L);
    }

    @Test
    void sendUpcomingReminders_appointmentWithin2Hours_shouldSend2hReminderOnly() {
        Appointment appt = appointment(2L, LocalDateTime.now().plusHours(1), CLIENT_CHAT);
        stubCandidates(appt);
        when(notificationRepository.existsByAppointmentIdAndType(2L, NotificationType.REMINDER_2H)).thenReturn(false);

        reminderService.sendUpcomingReminders();

        verify(messagingChannel).sendInlineKeyboard(eq(CLIENT_CHAT), contains("Recordatorio"), anyList());
        verify(notificationRepository).save(argThat(n -> n.getType() == NotificationType.REMINDER_2H));
        verify(notificationRepository, never()).existsByAppointmentIdAndType(eq(2L), eq(NotificationType.REMINDER_24H));
    }

    @Test
    void sendUpcomingReminders_reminderAlreadySent_shouldNotSendDuplicate() {
        Appointment appt = appointment(3L, LocalDateTime.now().plusHours(12), CLIENT_CHAT);
        stubCandidates(appt);
        when(notificationRepository.existsByAppointmentIdAndType(3L, NotificationType.REMINDER_24H)).thenReturn(true);

        reminderService.sendUpcomingReminders();

        verify(messagingChannel, never()).sendInlineKeyboard(anyString(), anyString(), anyList());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendUpcomingReminders_clientWithoutTelegramChatId_shouldNotSend() {
        Appointment appt = appointment(4L, LocalDateTime.now().plusHours(12), null);
        stubCandidates(appt);

        reminderService.sendUpcomingReminders();

        verify(messagingChannel, never()).sendInlineKeyboard(anyString(), anyString(), anyList());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendUpcomingReminders_multipleTenants_shouldCheckEveryTenant() {
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(1L), tenant(2L)));
        when(appointmentRepository.findRemindable(eq(1L), anyList(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findRemindable(eq(2L), anyList(), any(), any())).thenReturn(List.of());

        reminderService.sendUpcomingReminders();

        verify(appointmentRepository).findRemindable(eq(1L), anyList(), any(), any());
        verify(appointmentRepository).findRemindable(eq(2L), anyList(), any(), any());
    }

    @Test
    void sendDailySummary_stylistWithAppointments_shouldSendSummaryAndMarkSent() {
        Stylist stylist = Stylist.builder().id(5L).name_stylist("John Doe").telegram_chat_id(STYLIST_CHAT).build();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(TENANT_ID)));
        when(stylistRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stylist));
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                eq(5L), eq(NotificationType.DAILY_SUMMARY), any())).thenReturn(false);

        Appointment appt = Appointment.builder()
                .id(7L)
                .startDate(LocalDateTime.now().withHour(10).withMinute(0))
                .status(AppointmentStatus.CONFIRMED)
                .client(Client.builder().id(10L).name_client("Alice").build())
                .service(Service.builder().id(1L).name_service("Haircut").build())
                .build();
        when(appointmentRepository.findStylistDay(eq(5L), eq(TENANT_ID), any(), any())).thenReturn(List.of(appt));

        reminderService.sendDailySummary();

        verify(messagingChannel).sendMessage(eq(STYLIST_CHAT), contains("Resumen del día"));
        verify(messagingChannel).sendMessage(eq(STYLIST_CHAT), contains("Haircut"));
        verify(messagingChannel).sendMessage(eq(STYLIST_CHAT), contains("Alice"));
        verify(notificationRepository).save(argThat(n -> n.getType() == NotificationType.DAILY_SUMMARY));
    }

    @Test
    void sendDailySummary_stylistWithoutChatId_shouldNotSend() {
        Stylist stylist = Stylist.builder().id(5L).name_stylist("John Doe").telegram_chat_id(null).build();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(TENANT_ID)));
        when(stylistRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stylist));

        reminderService.sendDailySummary();

        verify(messagingChannel, never()).sendMessage(anyString(), anyString());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendDailySummary_alreadySentToday_shouldNotSendDuplicate() {
        Stylist stylist = Stylist.builder().id(5L).name_stylist("John Doe").telegram_chat_id(STYLIST_CHAT).build();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(TENANT_ID)));
        when(stylistRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stylist));
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                eq(5L), eq(NotificationType.DAILY_SUMMARY), any())).thenReturn(true);

        reminderService.sendDailySummary();

        verify(messagingChannel, never()).sendMessage(anyString(), anyString());
        verify(notificationRepository, never()).save(any());
        verify(appointmentRepository, never()).findStylistDay(any(), any(), any(), any());
    }

    @Test
    void sendDailySummary_noAppointments_shouldSendEmptySummary() {
        Stylist stylist = Stylist.builder().id(5L).name_stylist("John Doe").telegram_chat_id(STYLIST_CHAT).build();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(TENANT_ID)));
        when(stylistRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stylist));
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                eq(5L), eq(NotificationType.DAILY_SUMMARY), any())).thenReturn(false);
        when(appointmentRepository.findStylistDay(eq(5L), eq(TENANT_ID), any(), any())).thenReturn(List.of());

        reminderService.sendDailySummary();

        verify(messagingChannel).sendMessage(eq(STYLIST_CHAT), contains("No tenés citas hoy"));
    }
}
