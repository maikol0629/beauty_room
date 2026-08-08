package com.mr.sb.beauty_room.services.implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.sb.beauty_room.dto.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.dto.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.dto.client.ClientResponseDto;
import com.mr.sb.beauty_room.dto.service.SalonServiceResponseDto;
import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import com.mr.sb.beauty_room.exceptions.AppointmentConflictException;
import com.mr.sb.beauty_room.services.IAppointmentService;
import com.mr.sb.beauty_room.services.IBlockedSlotService;
import com.mr.sb.beauty_room.services.IConversationStateService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.SalonServiceRepository;
import com.mr.sb.beauty_room.repository.StylistScheduleRepository;
import com.mr.sb.beauty_room.services.ITelegramAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
class TelegramUpdateHandlerTest {

    @Mock
    private IMessagingChannel channel;
    @Mock
    private IConversationStateService conversationStateService;
    @Mock
    private IAppointmentService appointmentService;
    @Mock
    private IBlockedSlotService blockedSlotService;
    @Mock
    private SalonServiceRepository serviceRepository;
    @Mock
    private StylistScheduleRepository stylistScheduleRepository;
    @Mock
    private ITelegramAccountService accountService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TelegramUpdateHandler handler;

    @AfterEach
    void clearTenantContext() {
        com.mr.sb.beauty_room.security.TenantInterceptor.clear();
    }

    private ConversationState state(String chatId, String step, String data, Long tenantId) {
        return ConversationState.builder()
                .chatId(chatId)
                .currentStep(step)
                .data(data)
                .tenantId(tenantId)
                .build();
    }

    private void stubGetOrCreate(ConversationState state) {
        when(conversationStateService.getOrCreate(state.getChatId())).thenReturn(state);
    }

    private void stubMessage(TelegramMessage msg) {
        when(channel.parseUpdate(any(Update.class))).thenReturn(Optional.of(msg));
    }

    @Test
    void deepLink_withValidTenantKey_shouldResolveTenantAndSendMenu() {
        stubMessage(new TelegramMessage("111111111", "/start salon-maria-001", "juan", "Juan", 123L, null));
        when(accountService.resolveTenant(any(TelegramMessage.class))).thenReturn(1L);
        ConversationState state = state("111111111", null, null, null);
        stubGetOrCreate(state);

        handler.handle(new Update());

        verify(channel).sendKeyboard(eq("111111111"), contains("Hola"),
                argThat(buttons -> buttons.contains("Agendar cita") && buttons.contains("Mis citas")));
        verify(conversationStateService).save(argThat(s ->
                Long.valueOf(1L).equals(s.getTenantId()) && "MENU".equals(s.getCurrentStep())));
        verify(accountService).resolveTenant(any(TelegramMessage.class));
    }

    @Test
    void startWithoutPayload_knownChatId_shouldResolveTenantViaClient() {
        stubMessage(new TelegramMessage("222222222", "/start", null, null, null, null));
        when(accountService.resolveTenant(any(TelegramMessage.class))).thenReturn(2L);
        ConversationState state = state("222222222", null, null, null);
        stubGetOrCreate(state);

        handler.handle(new Update());

        verify(channel).sendKeyboard(eq("222222222"), contains("Hola"),
                argThat(buttons -> buttons.contains("Cancelar cita")));
        verify(conversationStateService).save(argThat(s -> Long.valueOf(2L).equals(s.getTenantId())));
    }

    @Test
    void unknownChatId_shouldSendGuidanceMessageInsteadOfKeyboard() {
        stubMessage(new TelegramMessage("999", "/start", null, null, null, null));
        when(accountService.resolveTenant(any(TelegramMessage.class))).thenReturn(null);
        ConversationState state = state("999", null, null, null);
        stubGetOrCreate(state);

        handler.handle(new Update());

        verify(channel).sendMessage(eq("999"), contains("enlace de tu salón"));
        verify(channel, never()).sendKeyboard(anyString(), anyString(), anyList());
    }

    @Test
    void updateWithoutMessage_shouldReturnNullAndDoNothing() {
        when(channel.parseUpdate(any(Update.class))).thenReturn(Optional.empty());

        assertThat(handler.handle(new Update())).isNull();
        verify(channel, never()).sendMessage(anyString(), anyString());
        verify(channel, never()).sendKeyboard(anyString(), anyString(), anyList());
        verifyNoInteractions(conversationStateService, accountService, appointmentService);
    }

    @Test
    void menuText_shouldListServicesWithInlineKeyboard() {
        stubMessage(new TelegramMessage("111", "Agendar cita", "juan", "Juan", 123L, null));
        ConversationState state = state("111", "MENU", null, 1L);
        stubGetOrCreate(state);

        SalonService haircut = SalonService.builder().id(1L).nameService("Haircut").price(15.99f).duration(30).build();
        when(serviceRepository.findByTenantId(1L)).thenReturn(List.of(haircut));

        handler.handle(new Update());

        verify(channel).sendInlineKeyboard(eq("111"), contains("¿Qué servicio"),
                argThat(buttons -> buttons.stream().anyMatch(b -> b.callbackData().equals("SERVICE:1"))));
        verify(conversationStateService).save(argThat(s -> "CHOOSE_SERVICE".equals(s.getCurrentStep())));
    }

    @Test
    void selectService_shouldAskForDateWithAvailableDays() {
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "SERVICE:1"));
        ConversationState state = state("111", "CHOOSE_SERVICE", null, 1L);
        stubGetOrCreate(state);

        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").build();
        SalonService haircut = SalonService.builder().id(1L).nameService("Haircut").stylist(stylist).build();
        when(serviceRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(haircut));
        when(appointmentService.getAvailableSlots(anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(LocalTime.of(10, 0)));

        handler.handle(new Update());

        verify(channel).sendInlineKeyboard(eq("111"), contains("¿Qué día"),
                argThat(buttons -> buttons.stream().anyMatch(b -> b.callbackData().startsWith("DATE:"))));
        verify(conversationStateService).save(argThat(s -> "CHOOSE_DATE".equals(s.getCurrentStep())));
    }

    @Test
    void selectDate_shouldShowAvailableTimes() {
        String data = "{\"serviceId\":\"1\",\"stylistId\":\"5\"}";
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "DATE:2026-08-10"));
        ConversationState state = state("111", "CHOOSE_DATE", data, 1L);
        stubGetOrCreate(state);

        when(appointmentService.getAvailableSlots(5L, 1L, LocalDate.of(2026, 8, 10)))
                .thenReturn(List.of(LocalTime.of(10, 0), LocalTime.of(10, 30)));

        handler.handle(new Update());

        verify(channel).sendInlineKeyboard(eq("111"), contains("Elegí una hora"),
                argThat(buttons -> buttons.stream().anyMatch(b -> b.callbackData().equals("TIME:10:00"))));
        verify(conversationStateService).save(argThat(s -> "CHOOSE_TIME".equals(s.getCurrentStep())
                && s.getData().contains("2026-08-10")));
    }

    @Test
    void selectDate_withoutSlots_shouldAskAnotherDate() {
        String data = "{\"serviceId\":\"1\",\"stylistId\":\"5\"}";
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "DATE:2026-08-10"));
        ConversationState state = state("111", "CHOOSE_DATE", data, 1L);
        stubGetOrCreate(state);

        when(appointmentService.getAvailableSlots(5L, 1L, LocalDate.of(2026, 8, 10))).thenReturn(List.of());

        handler.handle(new Update());

        verify(channel).sendMessage(eq("111"), contains("Sin horarios disponibles"));
        verify(channel, never()).sendInlineKeyboard(eq("111"), contains("Elegí una hora"), anyList());
    }

    @Test
    void selectTime_shouldAskForConfirmation() {
        String data = "{\"serviceId\":\"1\",\"stylistId\":\"5\",\"date\":\"2026-08-10\"}";
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "TIME:10:00"));
        ConversationState state = state("111", "CHOOSE_TIME", data, 1L);
        stubGetOrCreate(state);

        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").build();
        SalonService haircut = SalonService.builder().id(1L).nameService("Haircut").build();
        when(serviceRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(haircut));
        when(accountService.findStylistByIdAndTenant(5L, 1L)).thenReturn(Optional.of(stylist));

        handler.handle(new Update());

        verify(channel).sendInlineKeyboard(eq("111"), contains("Confirmá tu cita"),
                argThat(buttons -> buttons.stream().anyMatch(b -> b.callbackData().equals("CONFIRM"))));
        verify(conversationStateService).save(argThat(s -> "CONFIRM".equals(s.getCurrentStep())));
    }

    @Test
    void confirm_existingClient_shouldSaveAppointment() {
        String data = "{\"serviceId\":\"1\",\"stylistId\":\"5\",\"date\":\"2026-08-10\",\"time\":\"10:00\"}";
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "CONFIRM"));
        ConversationState state = state("111", "CONFIRM", data, 1L);
        stubGetOrCreate(state);

        Client client = Client.builder().id(3L).build();
        when(accountService.ensureClient(any(TelegramMessage.class), eq(1L))).thenReturn(client);
        when(appointmentService.save(any(AppointmentSaveDto.class))).thenReturn(true);

        handler.handle(new Update());

        verify(appointmentService).save(argThat(dto ->
                dto.getClientId() == 3L && dto.getServiceId() == 1L && dto.getStylistId() == 5L));
        verify(channel).sendMessage(eq("111"), contains("¡Listo!"));
        verify(conversationStateService).save(argThat(s -> "MENU".equals(s.getCurrentStep())));
    }

    @Test
    void confirm_newChat_shouldCreateClientAndSaveAppointment() {
        String data = "{\"serviceId\":\"1\",\"stylistId\":\"5\",\"date\":\"2026-08-10\",\"time\":\"10:00\"}";
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "CONFIRM"));
        ConversationState state = state("111", "CONFIRM", data, 1L);
        stubGetOrCreate(state);

        Client created = Client.builder().id(3L).nameClient("Juan").telegramChatId("111")
                .tenant(Tenant.builder().id(1L).build()).build();
        when(accountService.ensureClient(any(TelegramMessage.class), eq(1L))).thenReturn(created);
        when(appointmentService.save(any(AppointmentSaveDto.class))).thenReturn(true);

        handler.handle(new Update());

        verify(accountService).ensureClient(any(TelegramMessage.class), eq(1L));
        verify(appointmentService).save(argThat(dto -> dto.getClientId() == 3L));
        verify(channel).sendMessage(eq("111"), contains("¡Listo!"));
    }

    @Test
    void confirm_conflict_shouldWarnAndRepromptTimes() {
        String data = "{\"serviceId\":\"1\",\"stylistId\":\"5\",\"date\":\"2026-08-10\",\"time\":\"10:00\"}";
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "CONFIRM"));
        ConversationState state = state("111", "CONFIRM", data, 1L);
        stubGetOrCreate(state);

        Client client = Client.builder().id(3L).build();
        when(accountService.ensureClient(any(TelegramMessage.class), eq(1L))).thenReturn(client);
        when(appointmentService.save(any(AppointmentSaveDto.class)))
                .thenThrow(new AppointmentConflictException("ya no disponible"));
        when(appointmentService.getAvailableSlots(5L, 1L, LocalDate.of(2026, 8, 10)))
                .thenReturn(List.of(LocalTime.of(11, 0)));

        handler.handle(new Update());

        verify(channel).sendMessage(eq("111"), contains("ya no está disponible"));
        verify(channel).sendInlineKeyboard(eq("111"), contains("Elegí una hora"), anyList());
    }

    @Test
    void misCitas_shouldListClientAppointments() {
        stubMessage(new TelegramMessage("111", "Mis citas", "juan", "Juan", 123L, null));
        ConversationState state = state("111", "MENU", null, 1L);
        stubGetOrCreate(state);

        Client client = Client.builder().id(3L).build();
        when(accountService.findClientByChat(any(TelegramMessage.class), eq(1L))).thenReturn(Optional.of(client));
        AppointmentResponseDto appt = AppointmentResponseDto.builder()
                .id(7L)
                .startDate(LocalDateTime.of(2026, 8, 10, 10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .service(SalonServiceResponseDto.builder().name("Haircut").build())
                .build();
        when(appointmentService.findAppointmentsByClientID(3L)).thenReturn(List.of(appt));

        handler.handle(new Update());

        verify(channel).sendMessage(eq("111"), contains("Tus citas"));
        verify(channel).sendMessage(eq("111"), contains("Haircut"));
    }

    @Test
    void cancelarCita_shouldShowCancellableAppointments() {
        stubMessage(new TelegramMessage("111", "Cancelar cita", "juan", "Juan", 123L, null));
        ConversationState state = state("111", "MENU", null, 1L);
        stubGetOrCreate(state);

        Client client = Client.builder().id(3L).build();
        when(accountService.findClientByChat(any(TelegramMessage.class), eq(1L))).thenReturn(Optional.of(client));
        AppointmentResponseDto appt = AppointmentResponseDto.builder()
                .id(7L)
                .startDate(LocalDateTime.now().plusDays(2))
                .status(AppointmentStatus.PENDING)
                .service(SalonServiceResponseDto.builder().name("Haircut").build())
                .build();
        when(appointmentService.findAppointmentsByClientID(3L)).thenReturn(List.of(appt));

        handler.handle(new Update());

        verify(channel).sendInlineKeyboard(eq("111"), contains("¿Qué cita querés cancelar"),
                argThat(buttons -> buttons.stream().anyMatch(b -> b.callbackData().equals("CANCEL_APPT:7"))));
        verify(conversationStateService).save(argThat(s -> "CANCEL_SELECT".equals(s.getCurrentStep())));
    }

    @Test
    void cancelCallback_shouldCancelAppointment() {
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "CANCEL_APPT:7"));
        ConversationState state = state("111", "CANCEL_SELECT", null, 1L);
        stubGetOrCreate(state);

        when(appointmentService.cancelAppointment(7L)).thenReturn(true);

        handler.handle(new Update());

        verify(channel).sendMessage(eq("111"), contains("Cita cancelada"));
        verify(conversationStateService).save(argThat(s -> "MENU".equals(s.getCurrentStep())));
    }

    // ============================ ESTILISTA (Fase 4) ============================

    @Test
    void stylistMenu_shouldShowStylistOptions() {
        stubMessage(new TelegramMessage("111", "/start", "juan", "Juan", 123L, null));
        ConversationState state = state("111", null, null, null);
        stubGetOrCreate(state);

        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe")
                .tenant(Tenant.builder().id(1L).build()).build();
        when(accountService.resolveTenant(any(TelegramMessage.class))).thenReturn(1L);
        when(accountService.findStylistByChat(any(TelegramMessage.class), eq(1L))).thenReturn(Optional.of(stylist));

        handler.handle(new Update());

        verify(channel).sendKeyboard(eq("111"), contains("Hola"),
                argThat(buttons -> buttons.contains("Ver agenda")
                        && buttons.contains("Bloquear horario")
                        && buttons.contains("Gestionar citas")));
    }

    @Test
    void notStylist_shouldRejectAgendaCommand() {
        stubMessage(new TelegramMessage("111", "Ver agenda", "juan", "Juan", 123L, null));
        ConversationState state = state("111", "MENU", null, 1L);
        stubGetOrCreate(state);

        when(accountService.findStylistByChat(any(TelegramMessage.class), eq(1L))).thenReturn(Optional.empty());

        handler.handle(new Update());

        verify(channel).sendMessage(eq("111"), contains("solo para estilistas"));
        verify(appointmentService, never()).findAppointmentsByFilters(any(), any(), any(), any(), any());
    }

    @Test
    void stylistAgenda_shouldShowMenuAndTodayAppointments() {
        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").build();
        when(accountService.findStylistByChat(any(TelegramMessage.class), eq(1L))).thenReturn(Optional.of(stylist));

        stubMessage(new TelegramMessage("111", "Ver agenda", "juan", "Juan", 123L, null));
        ConversationState state = state("111", "MENU", null, 1L);
        stubGetOrCreate(state);

        handler.handle(new Update());

        verify(channel).sendInlineKeyboard(eq("111"), contains("¿Qué agenda"),
                argThat(buttons -> buttons.stream().anyMatch(b -> b.callbackData().equals("AGENDA_HOY"))
                        && buttons.stream().anyMatch(b -> b.callbackData().equals("AGENDA_SEMANA"))));

        AppointmentResponseDto appt = AppointmentResponseDto.builder()
                .id(7L)
                .startDate(LocalDateTime.now().withHour(10).withMinute(0))
                .status(AppointmentStatus.CONFIRMED)
                .service(SalonServiceResponseDto.builder().name("Haircut").build())
                .client(ClientResponseDto.builder().name("Alice").build())
                .build();
        when(appointmentService.findAppointmentsByFilters(eq(5L), any(), any(), any(), any()))
                .thenReturn(List.of(appt));

        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "AGENDA_HOY"));
        handler.handle(new Update());

        verify(channel).sendMessage(eq("111"), contains("Tu agenda"));
        verify(channel).sendMessage(eq("111"), contains("Haircut"));
    }

    @Test
    void stylistBlockFlow_shouldCreateBlockedSlot() {
        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").build();
        when(accountService.findStylistByChat(any(TelegramMessage.class), eq(1L))).thenReturn(Optional.of(stylist));

        ConversationState state = state("111", "BLOCK_DATE", "{\"stylistId\":\"5\"}", 1L);
        stubGetOrCreate(state);

        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "BLOCK_DATE:2026-08-10"));
        handler.handle(new Update());
        assertThat(state.getCurrentStep()).isEqualTo("BLOCK_START");

        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "BLOCK_START:10:00"));
        handler.handle(new Update());
        assertThat(state.getCurrentStep()).isEqualTo("BLOCK_END");

        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "BLOCK_END:12:00"));
        handler.handle(new Update());

        verify(blockedSlotService).create(argThat(dto ->
                dto.getStylistId() == 5L
                        && dto.getStartDate().equals(LocalDateTime.of(2026, 8, 10, 10, 0))
                        && dto.getEndDate().equals(LocalDateTime.of(2026, 8, 10, 12, 0))));
        verify(channel).sendMessage(eq("111"), contains("Horario bloqueado"));
    }

    @Test
    void stylistGestionar_shouldShowManagementButtons() {
        stubMessage(new TelegramMessage("111", "Gestionar citas", "juan", "Juan", 123L, null));
        ConversationState state = state("111", "MENU", null, 1L);
        stubGetOrCreate(state);

        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").build();
        when(accountService.findStylistByChat(any(TelegramMessage.class), eq(1L))).thenReturn(Optional.of(stylist));

        AppointmentResponseDto appt = AppointmentResponseDto.builder()
                .id(7L)
                .startDate(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                .status(AppointmentStatus.PENDING)
                .service(SalonServiceResponseDto.builder().name("Haircut").build())
                .client(ClientResponseDto.builder().name("Alice").build())
                .build();
        when(appointmentService.findAppointmentsByFilters(eq(5L), any(), any(), any(), any()))
                .thenReturn(List.of(appt));

        handler.handle(new Update());

        verify(channel).sendInlineKeyboard(eq("111"), contains("¿Qué querés hacer"),
                argThat(buttons -> buttons.stream().anyMatch(b -> b.callbackData().equals("APPT_COMPLETE:7"))
                        && buttons.stream().anyMatch(b -> b.callbackData().equals("APPT_NOSHOW:7"))
                        && buttons.stream().anyMatch(b -> b.callbackData().equals("APPT_CANCEL:7"))));
    }

    @Test
    void stylistCompleteCallback_shouldCompleteAppointment() {
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "APPT_COMPLETE:7"));
        ConversationState state = state("111", "STYLIST_APPT", null, 1L);
        stubGetOrCreate(state);

        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").build();
        when(accountService.findStylistByChat(any(TelegramMessage.class), eq(1L))).thenReturn(Optional.of(stylist));
        when(appointmentService.completeAppointment(7L)).thenReturn(true);

        handler.handle(new Update());

        verify(appointmentService).completeAppointment(7L);
        verify(channel).sendMessage(eq("111"), contains("Listo"));
        verify(conversationStateService).save(argThat(s -> "MENU".equals(s.getCurrentStep())));
    }

    @Test
    void stylistCancelCallback_shouldCancelAppointmentNotifyingClient() {
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L, "APPT_CANCEL:9"));
        ConversationState state = state("111", "STYLIST_APPT", null, 1L);
        stubGetOrCreate(state);

        Stylist stylist = Stylist.builder().id(5L).nameStylist("John Doe").build();
        when(accountService.findStylistByChat(any(TelegramMessage.class), eq(1L))).thenReturn(Optional.of(stylist));
        when(appointmentService.cancelAppointmentByStylist(9L)).thenReturn(true);

        handler.handle(new Update());

        verify(appointmentService).cancelAppointmentByStylist(9L);
        verify(channel).sendMessage(eq("111"), contains("Listo"));
    }

    // ============================ RECORDATORIOS (Fase 5) ============================

    @Test
    void reminderConfirmCallback_shouldConfirmAppointment() {
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L,
                ReminderServiceImplement.PREFIX_REMINDER_CONFIRM + "5"));
        ConversationState state = state("111", "MENU", null, 1L);
        stubGetOrCreate(state);

        when(appointmentService.confirmAppointment(5L)).thenReturn(true);

        handler.handle(new Update());

        verify(appointmentService).confirmAppointment(5L);
        verify(channel).sendMessage(eq("111"), contains("confirmar"));
        verify(conversationStateService).save(argThat(s -> "MENU".equals(s.getCurrentStep())));
    }

    @Test
    void reminderConfirmCallback_whenAlreadyConfirmed_shouldInformClient() {
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L,
                ReminderServiceImplement.PREFIX_REMINDER_CONFIRM + "5"));
        ConversationState state = state("111", "MENU", null, 1L);
        stubGetOrCreate(state);

        when(appointmentService.confirmAppointment(5L)).thenReturn(false);

        handler.handle(new Update());

        verify(appointmentService).confirmAppointment(5L);
        verify(channel).sendMessage(eq("111"), contains("No pudimos confirmar"));
    }

    @Test
    void reminderCancelCallback_shouldCancelAppointment() {
        stubMessage(new TelegramMessage("111", null, "juan", "Juan", 123L,
                ReminderServiceImplement.PREFIX_REMINDER_CANCEL + "5"));
        ConversationState state = state("111", "MENU", null, 1L);
        stubGetOrCreate(state);

        when(appointmentService.cancelAppointment(5L)).thenReturn(true);

        handler.handle(new Update());

        verify(appointmentService).cancelAppointment(5L);
        verify(channel).sendMessage(eq("111"), contains("cancelada"));
        verify(conversationStateService).save(argThat(s -> "MENU".equals(s.getCurrentStep())));
    }
}
