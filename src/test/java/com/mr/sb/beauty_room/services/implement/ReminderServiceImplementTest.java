package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.entities.NotificationType;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.NotificationRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.services.IConversationStateService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock
    private IConversationStateService conversationStateService;

    private ReminderServiceImplement reminderService;

    private Tenant tenant(long id) {
        return Tenant.builder().id(id).build();
    }

    @BeforeEach
    void setUp() {
        ConversationStateHelper stateHelper = new ConversationStateHelper(conversationStateService, new ObjectMapper());
        reminderService = new ReminderServiceImplement(
                tenantRepository,
                appointmentRepository,
                notificationRepository,
                stylistRepository,
                conversationStateService,
                stateHelper,
                messagingChannel);
    }

    private Appointment appointment(long id, LocalDateTime start, String clientChat) {
        Client client = Client.builder().id(10L).telegramChatId(clientChat).build();
        SalonService service = SalonService.builder().id(1L).nameService("Haircut").build();
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

    private void stubConversationState() {
        when(conversationStateService.getOrCreate(anyString())).thenReturn(new ConversationState());
        when(conversationStateService.save(any(ConversationState.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void sendUpcomingReminders_appointmentIn24hWindow_shouldSend24hReminderAndMarkSent() {
        Appointment appt = appointment(1L, LocalDateTime.now().plusHours(12), CLIENT_CHAT);
        stubCandidates(appt);
        stubConversationState();
        when(notificationRepository.existsByAppointmentIdAndType(1L, NotificationType.REMINDER_24H)).thenReturn(false);

        reminderService.sendUpcomingReminders();

        verify(messagingChannel).sendTemplate(eq(CLIENT_CHAT), argThat(template -> {
            assertThat(template.name()).isEqualTo("appointment_reminder_24h");
            assertThat(template.fallbackText()).contains("Recordatorio");
            assertThat(template.buttons()).anyMatch(b -> b.callbackData().equals(ReminderServiceImplement.PREFIX_REMINDER_CONFIRM + "1"));
            assertThat(template.buttons()).anyMatch(b -> b.callbackData().equals(ReminderServiceImplement.PREFIX_REMINDER_CANCEL + "1"));
            return true;
        }));
        verify(conversationStateService).save(argThat(state ->
                state.getData() != null && state.getData().contains(ReminderServiceImplement.DATA_REMINDER_APPT_ID)));
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
        stubConversationState();
        when(notificationRepository.existsByAppointmentIdAndType(2L, NotificationType.REMINDER_2H)).thenReturn(false);

        reminderService.sendUpcomingReminders();

        verify(messagingChannel).sendTemplate(eq(CLIENT_CHAT), argThat(template -> template.name().equals("appointment_reminder_2h")));
        verify(notificationRepository).save(argThat(n -> n.getType() == NotificationType.REMINDER_2H));
        verify(notificationRepository, never()).existsByAppointmentIdAndType(eq(2L), eq(NotificationType.REMINDER_24H));
    }

    @Test
    void sendUpcomingReminders_reminderAlreadySent_shouldNotSendDuplicate() {
        Appointment appt = appointment(3L, LocalDateTime.now().plusHours(12), CLIENT_CHAT);
        stubCandidates(appt);
        when(notificationRepository.existsByAppointmentIdAndType(3L, NotificationType.REMINDER_24H)).thenReturn(true);

        reminderService.sendUpcomingReminders();

        verify(messagingChannel, never()).sendTemplate(anyString(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendUpcomingReminders_clientWithoutChatId_shouldNotSend() {
        Appointment appt = appointment(4L, LocalDateTime.now().plusHours(12), null);
        stubCandidates(appt);

        reminderService.sendUpcomingReminders();

        verify(messagingChannel, never()).sendTemplate(anyString(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendUpcomingReminders_clientWithWhatsappChatId_shouldSendToWhatsappFirst() {
        Client client = Client.builder()
                .id(10L)
                .telegramChatId("tg-chat")
                .whatsappChatId("5491101234567")
                .build();
        SalonService service = SalonService.builder().id(1L).nameService("Haircut").build();
        Appointment appt = Appointment.builder()
                .id(5L)
                .startDate(LocalDateTime.now().plusHours(12))
                .status(AppointmentStatus.CONFIRMED)
                .client(client)
                .service(service)
                .tenant(tenant(TENANT_ID))
                .build();
        stubCandidates(appt);
        stubConversationState();
        when(notificationRepository.existsByAppointmentIdAndType(5L, NotificationType.REMINDER_24H)).thenReturn(false);

        reminderService.sendUpcomingReminders();

        verify(messagingChannel).sendTemplate(eq("5491101234567"), any());
        verify(messagingChannel, never()).sendTemplate(eq("tg-chat"), any());
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
        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").telegramChatId(STYLIST_CHAT).build();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(TENANT_ID)));
        when(stylistRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stylist));
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                eq(5L), eq(NotificationType.DAILY_SUMMARY), any())).thenReturn(false);

        Appointment appt = Appointment.builder()
                .id(7L)
                .startDate(LocalDateTime.now().withHour(10).withMinute(0))
                .status(AppointmentStatus.CONFIRMED)
                .client(Client.builder().id(10L).nameClient("Alice").build())
                .service(SalonService.builder().id(1L).nameService("Haircut").build())
                .build();
        when(appointmentRepository.findStylistDay(eq(5L), eq(TENANT_ID), any(), any())).thenReturn(List.of(appt));

        reminderService.sendDailySummary();

        verify(messagingChannel).sendTemplate(eq(STYLIST_CHAT), argThat(template -> template.name().equals("daily_summary")));
        verify(notificationRepository).save(argThat(n -> n.getType() == NotificationType.DAILY_SUMMARY));
    }

    @Test
    void sendDailySummary_stylistWithoutChatId_shouldNotSend() {
        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").telegramChatId(null).build();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(TENANT_ID)));
        when(stylistRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stylist));

        reminderService.sendDailySummary();

        verify(messagingChannel, never()).sendTemplate(anyString(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendDailySummary_alreadySentToday_shouldNotSendDuplicate() {
        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").telegramChatId(STYLIST_CHAT).build();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(TENANT_ID)));
        when(stylistRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stylist));
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                eq(5L), eq(NotificationType.DAILY_SUMMARY), any())).thenReturn(true);

        reminderService.sendDailySummary();

        verify(messagingChannel, never()).sendTemplate(anyString(), any());
        verify(notificationRepository, never()).save(any());
        verify(appointmentRepository, never()).findStylistDay(any(), any(), any(), any());
    }

    @Test
    void sendDailySummary_noAppointments_shouldSendEmptySummary() {
        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").telegramChatId(STYLIST_CHAT).build();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(TENANT_ID)));
        when(stylistRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stylist));
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                eq(5L), eq(NotificationType.DAILY_SUMMARY), any())).thenReturn(false);
        when(appointmentRepository.findStylistDay(eq(5L), eq(TENANT_ID), any(), any())).thenReturn(List.of());

        reminderService.sendDailySummary();

        verify(messagingChannel).sendTemplate(eq(STYLIST_CHAT), argThat(template -> template.fallbackText().contains("No tenés citas hoy")));
    }
}
