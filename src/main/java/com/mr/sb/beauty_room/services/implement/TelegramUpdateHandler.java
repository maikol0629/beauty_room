package com.mr.sb.beauty_room.services.implement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.sb.beauty_room.dto.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.dto.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.dto.blockedslot.BlockedSlotRequestDto;
import com.mr.sb.beauty_room.dto.telegram.Button;
import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import com.mr.sb.beauty_room.exceptions.AppointmentConflictException;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.services.IAppointmentService;
import com.mr.sb.beauty_room.services.IBlockedSlotService;
import com.mr.sb.beauty_room.services.IConversationStateService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.StylistSchedule;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.SalonServiceRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.StylistScheduleRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateHandler.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String STEP_MENU = "MENU";
    private static final String STEP_INITIAL = "INITIAL";
    private static final String STEP_CHOOSE_SERVICE = "CHOOSE_SERVICE";
    private static final String STEP_CHOOSE_DATE = "CHOOSE_DATE";
    private static final String STEP_CHOOSE_TIME = "CHOOSE_TIME";
    private static final String STEP_CONFIRM = "CONFIRM";
    private static final String STEP_CANCEL_SELECT = "CANCEL_SELECT";
    private static final String STEP_BLOCK_DATE = "BLOCK_DATE";
    private static final String STEP_BLOCK_START = "BLOCK_START";
    private static final String STEP_BLOCK_END = "BLOCK_END";
    private static final String STEP_STYLIST_APPT = "STYLIST_APPT";

    private static final String CB_MENU = "MENU";
    private static final String CB_AGENDAR = "AGENDAR";
    private static final String CB_MIS_CITAS = "MIS_CITAS";
    private static final String CB_CANCELAR_CITA = "CANCELAR_CITA";
    private static final String CB_CONFIRM = "CONFIRM";
    private static final String CB_ABORT = "ABORT";
    private static final String CB_BACK_DATES = "BACK_DATES";
    private static final String CB_AGENDA_HOY = "AGENDA_HOY";
    private static final String CB_AGENDA_SEMANA = "AGENDA_SEMANA";
    private static final String PREFIX_SERVICE = "SERVICE:";
    private static final String PREFIX_DATE = "DATE:";
    private static final String PREFIX_TIME = "TIME:";
    private static final String PREFIX_CANCEL_APPT = "CANCEL_APPT:";
    private static final String PREFIX_BLOCK_DATE = "BLOCK_DATE:";
    private static final String PREFIX_BLOCK_START = "BLOCK_START:";
    private static final String PREFIX_BLOCK_END = "BLOCK_END:";
    private static final String PREFIX_APPT_COMPLETE = "APPT_COMPLETE:";
    private static final String PREFIX_APPT_NOSHOW = "APPT_NOSHOW:";
    private static final String PREFIX_APPT_CANCEL = "APPT_CANCEL:";
    private static final String PREFIX_REMINDER_CONFIRM = ReminderServiceImplement.PREFIX_REMINDER_CONFIRM;
    private static final String PREFIX_REMINDER_CANCEL = ReminderServiceImplement.PREFIX_REMINDER_CANCEL;

    private final IMessagingChannel channel;
    private final IConversationStateService conversationStateService;
    private final IAppointmentService appointmentService;
    private final IBlockedSlotService blockedSlotService;
    private final ClientRepository clientRepository;
    private final TenantRepository tenantRepository;
    private final SalonServiceRepository serviceRepository;
    private final StylistRepository stylistRepository;
    private final StylistScheduleRepository stylistScheduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public BotApiMethod<?> handle(Update update) {
        Optional<TelegramMessage> parsed = channel.parseUpdate(update);
        if (parsed.isEmpty()) {
            return null;
        }
        TelegramMessage msg = parsed.get();
        log.info("Update entrante chat_id={} username={} text={} callback={}", msg.chatId(), msg.username(), msg.text(), msg.callbackData());

        ConversationState state = conversationStateService.getOrCreate(msg.chatId());

        if (msg.callbackData() != null) {
            handleCallback(msg, state);
        } else {
            handleText(msg, state);
        }
        return null;
    }

    // ============================ TEXTO ============================

    private void handleText(TelegramMessage msg, ConversationState state) {
        String text = msg.text() == null ? "" : msg.text().trim();
        Long tenantId = state.getTenantId() != null ? state.getTenantId() : resolveTenant(msg);
        if (tenantId != null && state.getTenantId() == null) {
            state.setTenantId(tenantId);
        }

        if (text.startsWith("/start")) {
            if (tenantId != null) {
                updateState(state, STEP_MENU, null);
                showMenu(msg, tenantId);
            } else {
                sendGuidance(msg);
                updateState(state, STEP_INITIAL, null);
            }
            return;
        }

        if (tenantId == null) {
            sendGuidance(msg);
            updateState(state, STEP_INITIAL, null);
            return;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (text.equalsIgnoreCase("/schedule") || text.equalsIgnoreCase("/agendar") || lower.equals("agendar cita")) {
            startSchedule(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/miscitas") || lower.equals("mis citas")) {
            showMyAppointments(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/cancel") || text.equalsIgnoreCase("/cancelar") || lower.equals("cancelar cita")) {
            startCancel(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/agenda") || lower.equals("ver agenda")) {
            showStylistAgendaMenu(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/bloquear") || lower.equals("bloquear") || lower.equals("bloquear horario")) {
            startBlock(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/gestionar") || text.equalsIgnoreCase("/estado") || lower.equals("gestionar citas")) {
            showStylistAppointments(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/reschedule")) {
            channel.sendMessage(msg.chatId(), "El reagendado llega pronto. Por ahora, cancelá la cita y agendá una nueva.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }

        Map<String, String> data = parseData(state.getData());
        String step = state.getCurrentStep();
        if (STEP_CHOOSE_DATE.equals(step)) {
            trySelectDateByText(msg, state, data, tenantId, text);
        } else if (STEP_CHOOSE_TIME.equals(step)) {
            trySelectTimeByText(msg, state, data, tenantId, text);
        } else {
            channel.sendMessage(msg.chatId(), "Elegí una opción del menú:");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
        }
    }

    private void trySelectDateByText(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, String text) {
        LocalDate date = parseDate(text);
        if (date == null) {
            channel.sendMessage(msg.chatId(), "No entendí la fecha. Usá el formato YYYY-MM-DD (ej: 2026-08-10) o elegí una de las fechas de abajo.");
            showDateOptions(msg, data, tenantId);
            return;
        }
        selectDate(msg, state, data, tenantId, date);
    }

    private void trySelectTimeByText(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, String text) {
        LocalTime time;
        try {
            time = LocalTime.parse(text, TIME_FMT);
        } catch (Exception e) {
            channel.sendMessage(msg.chatId(), "No entendí la hora. Usá el formato HH:mm (ej: 10:30) o elegí una de las horas de abajo.");
            showTimeOptions(msg, data, tenantId);
            return;
        }
        selectTime(msg, state, data, tenantId, time);
    }

    // ============================ CALLBACK ============================

    private void handleCallback(TelegramMessage msg, ConversationState state) {
        String cb = msg.callbackData();
        if (cb == null) {
            return;
        }
        Long tenantId = state.getTenantId() != null ? state.getTenantId() : resolveTenant(msg);
        if (tenantId != null && state.getTenantId() == null) {
            state.setTenantId(tenantId);
        }
        Map<String, String> data = parseData(state.getData());

        switch (cb) {
            case CB_MENU -> {
                showMenu(msg, tenantId);
                updateState(state, STEP_MENU, null);
            }
            case CB_AGENDAR -> {
                if (tenantId == null) {
                    sendGuidance(msg);
                } else {
                    startSchedule(msg, state, tenantId);
                }
            }
            case CB_MIS_CITAS -> {
                if (tenantId == null) {
                    sendGuidance(msg);
                } else {
                    showMyAppointments(msg, state, tenantId);
                }
            }
            case CB_CANCELAR_CITA -> {
                if (tenantId == null) {
                    sendGuidance(msg);
                } else {
                    startCancel(msg, state, tenantId);
                }
            }
            case CB_BACK_DATES -> {
                if (tenantId == null) {
                    sendGuidance(msg);
                } else {
                    showDateOptions(msg, data, tenantId);
                }
            }
            case CB_AGENDA_HOY -> {
                if (tenantId == null) {
                    sendGuidance(msg);
                } else {
                    showAgenda(msg, state, tenantId, LocalDate.now(), LocalDate.now());
                }
            }
            case CB_AGENDA_SEMANA -> {
                if (tenantId == null) {
                    sendGuidance(msg);
                } else {
                    showAgenda(msg, state, tenantId, LocalDate.now(), LocalDate.now().plusDays(7));
                }
            }
            case CB_CONFIRM -> {
                if (tenantId != null) {
                    confirmAppointment(msg, state, data, tenantId);
                } else {
                    sendGuidance(msg);
                }
            }
            case CB_ABORT -> {
                channel.sendMessage(msg.chatId(), "Listo, lo dejamos acá.");
                showMenu(msg, tenantId);
                updateState(state, STEP_MENU, null);
            }
            default -> {
                if (tenantId == null) {
                    sendGuidance(msg);
                    return;
                }
                if (cb.startsWith(PREFIX_SERVICE)) {
                    selectService(msg, state, tenantId, cb.substring(PREFIX_SERVICE.length()));
                } else if (cb.startsWith(PREFIX_DATE)) {
                    try {
                        selectDate(msg, state, data, tenantId, LocalDate.parse(cb.substring(PREFIX_DATE.length())));
                    } catch (Exception e) {
                        channel.sendMessage(msg.chatId(), "Fecha inválida. Elegí otra:");
                        showDateOptions(msg, data, tenantId);
                    }
                } else if (cb.startsWith(PREFIX_TIME)) {
                    try {
                        selectTime(msg, state, data, tenantId, LocalTime.parse(cb.substring(PREFIX_TIME.length()), TIME_FMT));
                    } catch (Exception e) {
                        channel.sendMessage(msg.chatId(), "Hora inválida. Elegí otra:");
                        showTimeOptions(msg, data, tenantId);
                    }
                } else if (cb.startsWith(PREFIX_CANCEL_APPT)) {
                    cancelAppointmentByCallback(msg, state, tenantId, cb.substring(PREFIX_CANCEL_APPT.length()));
                } else if (cb.startsWith(PREFIX_BLOCK_DATE)) {
                    try {
                        selectBlockDate(msg, state, data, tenantId, LocalDate.parse(cb.substring(PREFIX_BLOCK_DATE.length())));
                    } catch (Exception e) {
                        channel.sendMessage(msg.chatId(), "Fecha inválida. Elegí otra:");
                        startBlock(msg, state, tenantId);
                    }
                } else if (cb.startsWith(PREFIX_BLOCK_START)) {
                    try {
                        selectBlockStart(msg, state, data, tenantId, LocalTime.parse(cb.substring(PREFIX_BLOCK_START.length()), TIME_FMT));
                    } catch (Exception e) {
                        channel.sendMessage(msg.chatId(), "Hora inválida. Elegí otra:");
                        showBlockStartOptions(msg, data, tenantId);
                    }
                } else if (cb.startsWith(PREFIX_BLOCK_END)) {
                    try {
                        selectBlockEnd(msg, state, data, tenantId, LocalTime.parse(cb.substring(PREFIX_BLOCK_END.length()), TIME_FMT));
                    } catch (Exception e) {
                        channel.sendMessage(msg.chatId(), "Hora inválida. Elegí otra:");
                        showBlockEndOptions(msg, data, tenantId);
                    }
                } else if (cb.startsWith(PREFIX_APPT_COMPLETE)) {
                    manageAppointmentByCallback(msg, state, tenantId, PREFIX_APPT_COMPLETE, cb.substring(PREFIX_APPT_COMPLETE.length()));
                } else if (cb.startsWith(PREFIX_APPT_NOSHOW)) {
                    manageAppointmentByCallback(msg, state, tenantId, PREFIX_APPT_NOSHOW, cb.substring(PREFIX_APPT_NOSHOW.length()));
                } else if (cb.startsWith(PREFIX_APPT_CANCEL)) {
                    manageAppointmentByCallback(msg, state, tenantId, PREFIX_APPT_CANCEL, cb.substring(PREFIX_APPT_CANCEL.length()));
                } else if (cb.startsWith(PREFIX_REMINDER_CONFIRM)) {
                    handleReminderConfirm(msg, state, tenantId, cb.substring(PREFIX_REMINDER_CONFIRM.length()));
                } else if (cb.startsWith(PREFIX_REMINDER_CANCEL)) {
                    handleReminderCancel(msg, state, tenantId, cb.substring(PREFIX_REMINDER_CANCEL.length()));
                } else {
                    channel.sendMessage(msg.chatId(), "Opción desconocida. Abrí el menú:");
                    showMenu(msg, tenantId);
                    updateState(state, STEP_MENU, null);
                }
            }
        }
    }

    // ============================ FLUJO: AGENDAR ============================

    private void startSchedule(TelegramMessage msg, ConversationState state, Long tenantId) {
        List<SalonService> services = serviceRepository.findByTenantId(tenantId);
        if (services.isEmpty()) {
            channel.sendMessage(msg.chatId(), "Todavía no hay servicios cargados en tu salón. Probá más tarde.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        List<Button> buttons = services.stream()
                .map(s -> new Button(s.getNameService() + " · $" + s.getPrice() + " (" + s.getDuration() + " min)",
                        PREFIX_SERVICE + s.getId()))
                .toList();
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué servicio querés agendar?", buttons);
        updateState(state, STEP_CHOOSE_SERVICE, new HashMap<>());
    }

    private void selectService(TelegramMessage msg, ConversationState state, Long tenantId, String serviceIdText) {
        Long serviceId;
        try {
            serviceId = Long.parseLong(serviceIdText);
        } catch (NumberFormatException e) {
            channel.sendMessage(msg.chatId(), "Servicio inválido. Elegí uno de la lista:");
            return;
        }
        SalonService service = serviceRepository.findByIdAndTenantId(serviceId, tenantId).orElse(null);
        if (service == null || service.getStylist() == null) {
            channel.sendMessage(msg.chatId(), "Ese servicio ya no está disponible. Elegí otro:");
            startSchedule(msg, state, tenantId);
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("serviceId", String.valueOf(service.getId()));
        data.put("stylistId", String.valueOf(service.getStylist().getId()));
        channel.sendMessage(msg.chatId(), "Perfecto, " + service.getNameService() + ".");
        showDateOptions(msg, data, tenantId);
        updateState(state, STEP_CHOOSE_DATE, data);
    }

    private void showDateOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
        String stylistId = data.get("stylistId");
        String serviceId = data.get("serviceId");
        if (stylistId == null || serviceId == null) {
            channel.sendMessage(msg.chatId(), "Perdimos la selección. Empecemos de nuevo:");
            showMenu(msg, tenantId);
            return;
        }
        List<LocalDate> dates = findDatesWithSlots(Long.parseLong(stylistId), Long.parseLong(serviceId), 7);
        if (dates.isEmpty()) {
            channel.sendMessage(msg.chatId(),
                    "No hay disponibilidad en los próximos 7 días. Escribí una fecha manual (YYYY-MM-DD) o probá más tarde.");
            return;
        }
        List<Button> buttons = dates.stream()
                .map(d -> new Button(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es")) + " " + d,
                        PREFIX_DATE + d))
                .toList();
        List<Button> all = new ArrayList<>(buttons);
        all.add(new Button("◀ Volver", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué día te viene bien?", all);
    }

    private List<LocalDate> findDatesWithSlots(Long stylistId, Long serviceId, int days) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= days; i++) {
            LocalDate candidate = today.plusDays(i);
            if (!appointmentService.getAvailableSlots(stylistId, serviceId, candidate).isEmpty()) {
                dates.add(candidate);
            }
        }
        return dates;
    }

    private void selectDate(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            channel.sendMessage(msg.chatId(), "Esa fecha ya pasó. Elegí otra:");
            showDateOptions(msg, data, tenantId);
            return;
        }
        String stylistId = data.get("stylistId");
        String serviceId = data.get("serviceId");
        if (stylistId == null || serviceId == null) {
            channel.sendMessage(msg.chatId(), "Perdimos la selección. Empecemos de nuevo:");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        List<LocalTime> slots = appointmentService.getAvailableSlots(Long.parseLong(stylistId), Long.parseLong(serviceId), date);
        if (slots.isEmpty()) {
            channel.sendMessage(msg.chatId(), "Sin horarios disponibles el " + date + ". Elegí otra fecha:");
            showDateOptions(msg, data, tenantId);
            return;
        }
        data.put("date", date.toString());
        channel.sendMessage(msg.chatId(), "Horarios disponibles el " + date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es")) + " " + date + ":");
        showTimeOptions(msg, data, tenantId);
        updateState(state, STEP_CHOOSE_TIME, data);
    }

    private void showTimeOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
        String dateStr = data.get("date");
        if (dateStr == null) {
            showDateOptions(msg, data, tenantId);
            return;
        }
        LocalDate date = LocalDate.parse(dateStr);
        List<LocalTime> slots = appointmentService.getAvailableSlots(
                Long.parseLong(data.get("stylistId")), Long.parseLong(data.get("serviceId")), date);
        if (slots.isEmpty()) {
            channel.sendMessage(msg.chatId(), "Sin horarios disponibles el " + date + ". Elegí otra fecha:");
            showDateOptions(msg, data, tenantId);
            return;
        }
        List<Button> buttons = slots.stream()
                .map(t -> new Button(t.format(TIME_FMT), PREFIX_TIME + t))
                .toList();
        List<Button> all = new ArrayList<>(buttons);
        all.add(new Button("◀ Otra fecha", CB_BACK_DATES));
        channel.sendInlineKeyboard(msg.chatId(), "Elegí una hora:", all);
    }

    private void selectTime(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalTime time) {
        String serviceId = data.get("serviceId");
        String stylistId = data.get("stylistId");
        String dateStr = data.get("date");
        if (serviceId == null || stylistId == null || dateStr == null) {
            channel.sendMessage(msg.chatId(), "Perdimos la selección. Empecemos de nuevo:");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        SalonService service = serviceRepository.findByIdAndTenantId(Long.parseLong(serviceId), tenantId).orElse(null);
        Stylist stylist = stylistRepository.findByIdAndTenantId(Long.parseLong(stylistId), tenantId).orElse(null);
        data.put("time", time.toString());

        String text = "📋 Confirmá tu cita:\n\n"
                + "• Servicio: " + (service != null ? service.getNameService() : serviceId) + "\n"
                + "• Estilista: " + (stylist != null ? stylist.getNameStylist() : stylistId) + "\n"
                + "• Fecha: " + dateStr + "\n"
                + "• Hora: " + time.format(TIME_FMT) + "\n\n"
                + "¿Todo correcto?";
        channel.sendInlineKeyboard(msg.chatId(), text, List.of(
                new Button("✅ Confirmar", CB_CONFIRM),
                new Button("❌ Cancelar", CB_ABORT)));
        updateState(state, STEP_CONFIRM, data);
    }

    private void confirmAppointment(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId) {
        String serviceId = data.get("serviceId");
        String stylistId = data.get("stylistId");
        String dateStr = data.get("date");
        String timeStr = data.get("time");
        if (serviceId == null || stylistId == null || dateStr == null || timeStr == null) {
            channel.sendMessage(msg.chatId(), "Perdimos el hilo de la conversación. Empecemos de nuevo:");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }

        Client client = ensureClient(msg, tenantId);
        AppointmentSaveDto dto = AppointmentSaveDto.builder()
                .startDate(LocalDate.parse(dateStr).atTime(LocalTime.parse(timeStr)))
                .clientId(client.getId())
                .stylistId(Long.parseLong(stylistId))
                .serviceId(Long.parseLong(serviceId))
                .build();

        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            boolean ok = appointmentService.save(dto);
            if (ok) {
                channel.sendMessage(msg.chatId(),
                        "✅ ¡Listo! Tu cita quedó agendada:\n\n"
                                + "• Fecha: " + LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n"
                                + "• Hora: " + timeStr + "\n\n"
                                + "Te esperamos.");
                updateState(state, STEP_MENU, null);
            } else {
                channel.sendMessage(msg.chatId(), "No se pudo agendar la cita. Elegí otra hora:");
                showTimeOptions(msg, data, tenantId);
                updateState(state, STEP_CHOOSE_TIME, data);
            }
        } catch (AppointmentConflictException e) {
            log.info("Conflicto de agenda para chat_id={}: {}", msg.chatId(), e.getMessage());
            channel.sendMessage(msg.chatId(), "⚠️ Ese horario ya no está disponible. Elegí otra hora:");
            showTimeOptions(msg, data, tenantId);
            updateState(state, STEP_CHOOSE_TIME, data);
        } catch (Exception e) {
            log.error("Error agendando cita para chat_id={}: {}", msg.chatId(), e.getMessage(), e);
            channel.sendMessage(msg.chatId(), "Ocurrió un error al agendar. Intentá de nuevo.");
            updateState(state, STEP_MENU, null);
        } finally {
            TenantInterceptor.clear();
        }
    }

    // ============================ FLUJO: MIS CITAS ============================

    private void showMyAppointments(TelegramMessage msg, ConversationState state, Long tenantId) {
        Optional<Client> clientOpt = clientRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId);
        if (clientOpt.isEmpty()) {
            channel.sendMessage(msg.chatId(), "Todavía no tenés citas. Agendá una tocando «Agendar cita».");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        List<AppointmentResponseDto> appointments;
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            appointments = appointmentService.findAppointmentsByClientID(clientOpt.get().getId());
        } finally {
            TenantInterceptor.clear();
        }
        if (appointments.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No tenés citas registradas.");
        } else {
            List<AppointmentResponseDto> sorted = appointments.stream()
                    .sorted((a, b) -> b.getStartDate().compareTo(a.getStartDate()))
                    .toList();
            StringBuilder sb = new StringBuilder("📅 Tus citas:\n\n");
            for (AppointmentResponseDto a : sorted) {
                sb.append("• ").append(a.getStartDate().format(DATETIME_FMT))
                        .append(" — ").append(a.getService().getName())
                        .append(" (").append(a.getStatus()).append(")\n");
            }
            channel.sendMessage(msg.chatId(), sb.toString());
        }
        showMenu(msg, tenantId);
        updateState(state, STEP_MENU, null);
    }

    // ============================ FLUJO: CANCELAR ============================

    private void startCancel(TelegramMessage msg, ConversationState state, Long tenantId) {
        Optional<Client> clientOpt = clientRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId);
        if (clientOpt.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No tenés citas para cancelar. Agendá una tocando «Agendar cita».");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        List<AppointmentResponseDto> appointments;
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            appointments = appointmentService.findAppointmentsByClientID(clientOpt.get().getId());
        } finally {
            TenantInterceptor.clear();
        }
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentResponseDto> cancellable = appointments.stream()
                .filter(a -> (a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                        && a.getStartDate().isAfter(now))
                .toList();
        if (cancellable.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No tenés citas activas para cancelar.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        List<Button> buttons = cancellable.stream()
                .map(a -> new Button("Cancelar " + a.getStartDate().format(DATETIME_FMT) + " (" + a.getService().getName() + ")",
                        PREFIX_CANCEL_APPT + a.getId()))
                .toList();
        List<Button> all = new ArrayList<>(buttons);
        all.add(new Button("◀ Volver", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué cita querés cancelar?", all);
        updateState(state, STEP_CANCEL_SELECT, null);
    }

    private void cancelAppointmentByCallback(TelegramMessage msg, ConversationState state, Long tenantId, String appointmentIdText) {
        Long appointmentId;
        try {
            appointmentId = Long.parseLong(appointmentIdText);
        } catch (NumberFormatException e) {
            channel.sendMessage(msg.chatId(), "Cita inválida.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        boolean ok;
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            ok = appointmentService.cancelAppointment(appointmentId);
        } catch (Exception e) {
            log.error("Error cancelando cita {} para chat_id={}: {}", appointmentId, msg.chatId(), e.getMessage(), e);
            ok = false;
        } finally {
            TenantInterceptor.clear();
        }
        channel.sendMessage(msg.chatId(), ok
                ? "✅ Cita cancelada."
                : "No se pudo cancelar esa cita (¿ya está cancelada o no es tuya?).");
        showMenu(msg, tenantId);
        updateState(state, STEP_MENU, null);
    }

    // ============================ FLUJO: ESTILISTA ============================

    private Optional<Stylist> ensureStylist(TelegramMessage msg, Long tenantId) {
        return stylistRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId);
    }

    // ----- AGENDA DEL ESTILISTA -----

    private void showStylistAgendaMenu(TelegramMessage msg, ConversationState state, Long tenantId) {
        if (ensureStylist(msg, tenantId).isEmpty()) {
            channel.sendMessage(msg.chatId(), "Este comando es solo para estilistas.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué agenda querés ver?", List.of(
                new Button("📅 Hoy", CB_AGENDA_HOY),
                new Button("🗓 Próximos 7 días", CB_AGENDA_SEMANA),
                new Button("◀ Volver", CB_MENU)));
        updateState(state, STEP_MENU, null);
    }

    private void showAgenda(TelegramMessage msg, ConversationState state, Long tenantId, LocalDate from, LocalDate to) {
        Optional<Stylist> stylistOpt = ensureStylist(msg, tenantId);
        if (stylistOpt.isEmpty()) {
            channel.sendMessage(msg.chatId(), "Este comando es solo para estilistas.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        List<AppointmentResponseDto> appointments;
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            appointments = appointmentService.findAppointmentsByFilters(
                    stylistOpt.get().getId(), null, null, from.atStartOfDay(), to.atTime(LocalTime.MAX));
        } finally {
            TenantInterceptor.clear();
        }
        List<AppointmentResponseDto> filtered = appointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.REJECTED)
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .toList();
        if (filtered.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No tenés citas en ese período.");
        } else {
            StringBuilder sb = new StringBuilder("📋 Tu agenda:\n\n");
            for (AppointmentResponseDto a : filtered) {
                sb.append("• ").append(a.getStartDate().format(DATETIME_FMT))
                        .append(" — ").append(a.getService() != null ? a.getService().getName() : "?")
                        .append(" (").append(a.getClient() != null ? a.getClient().getName() : "?")
                        .append(") [").append(a.getStatus()).append("]\n");
            }
            channel.sendMessage(msg.chatId(), sb.toString());
        }
        showMenu(msg, tenantId);
        updateState(state, STEP_MENU, null);
    }

    // ----- BLOQUEAR HORARIO -----

    private void startBlock(TelegramMessage msg, ConversationState state, Long tenantId) {
        Optional<Stylist> stylistOpt = ensureStylist(msg, tenantId);
        if (stylistOpt.isEmpty()) {
            channel.sendMessage(msg.chatId(), "Este comando es solo para estilistas.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("stylistId", String.valueOf(stylistOpt.get().getId()));
        List<Button> buttons = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.plusDays(i);
            buttons.add(new Button(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es")) + " " + d,
                    PREFIX_BLOCK_DATE + d));
        }
        buttons.add(new Button("◀ Volver", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué día querés bloquear?", buttons);
        updateState(state, STEP_BLOCK_DATE, data);
    }

    private void selectBlockDate(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            channel.sendMessage(msg.chatId(), "Esa fecha ya pasó. Elegí otra:");
            startBlock(msg, state, tenantId);
            return;
        }
        data.put("date", date.toString());
        showBlockStartOptions(msg, data, tenantId);
        updateState(state, STEP_BLOCK_START, data);
    }

    private void showBlockStartOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
        List<LocalTime> times = buildBlockTimes(Long.parseLong(data.get("stylistId")), LocalDate.parse(data.get("date")), tenantId);
        if (times.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No hay horarios para bloquear ese día. Elegí otro:");
            return;
        }
        List<Button> buttons = new ArrayList<>(times.stream()
                .map(t -> new Button(t.format(TIME_FMT), PREFIX_BLOCK_START + t))
                .toList());
        buttons.add(new Button("◀ Otro día", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Desde qué hora?", buttons);
    }

    private void selectBlockStart(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalTime start) {
        LocalDate date = LocalDate.parse(data.get("date"));
        if (date.atTime(start).isBefore(LocalDateTime.now())) {
            channel.sendMessage(msg.chatId(), "Esa hora ya pasó. Elegí otra hora de inicio:");
            showBlockStartOptions(msg, data, tenantId);
            return;
        }
        data.put("start", start.toString());
        showBlockEndOptions(msg, data, tenantId);
        updateState(state, STEP_BLOCK_END, data);
    }

    private void showBlockEndOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
        List<LocalTime> times = buildBlockEndTimes(
                Long.parseLong(data.get("stylistId")), LocalDate.parse(data.get("date")),
                LocalTime.parse(data.get("start")), tenantId);
        if (times.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No hay horas de fin disponibles desde esa hora. Elegí otro inicio:");
            showBlockStartOptions(msg, data, tenantId);
            return;
        }
        List<Button> buttons = new ArrayList<>(times.stream()
                .map(t -> new Button(t.format(TIME_FMT), PREFIX_BLOCK_END + t))
                .toList());
        buttons.add(new Button("◀ Cambiar inicio", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Hasta qué hora?", buttons);
    }

    private void selectBlockEnd(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalTime end) {
        Long stylistId = Long.parseLong(data.get("stylistId"));
        LocalDate date = LocalDate.parse(data.get("date"));
        LocalTime start = LocalTime.parse(data.get("start"));
        if (!end.isAfter(start)) {
            channel.sendMessage(msg.chatId(), "La hora de fin debe ser posterior a la de inicio:");
            showBlockEndOptions(msg, data, tenantId);
            return;
        }
        BlockedSlotRequestDto dto = BlockedSlotRequestDto.builder()
                .stylistId(stylistId)
                .startDate(date.atTime(start))
                .endDate(date.atTime(end))
                .reason("Bloqueado desde el bot")
                .build();
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            blockedSlotService.create(dto);
            channel.sendMessage(msg.chatId(),
                    "✅ Horario bloqueado: " + date + " de " + start.format(TIME_FMT) + " a " + end.format(TIME_FMT) + ".");
        } catch (Exception e) {
            log.error("Error bloqueando horario para chat_id={}: {}", msg.chatId(), e.getMessage(), e);
            channel.sendMessage(msg.chatId(), "No se pudo bloquear ese horario. Intentá de nuevo.");
        } finally {
            TenantInterceptor.clear();
        }
        showMenu(msg, tenantId);
        updateState(state, STEP_MENU, null);
    }

    private List<LocalTime> buildBlockTimes(Long stylistId, LocalDate date, Long tenantId) {
        List<StylistSchedule> schedules = stylistScheduleRepository
                .findByStylistIdAndDayAndTenantId(stylistId, date.getDayOfWeek(), tenantId);
        LocalTime dayStart = schedules.isEmpty()
                ? LocalTime.of(8, 0)
                : schedules.stream().map(StylistSchedule::getStartTime).min(LocalTime::compareTo).orElse(LocalTime.of(8, 0));
        LocalTime dayEnd = schedules.isEmpty()
                ? LocalTime.of(20, 0)
                : schedules.stream().map(StylistSchedule::getEndTime).max(LocalTime::compareTo).orElse(LocalTime.of(20, 0));
        List<LocalTime> times = new ArrayList<>();
        LocalTime t = dayStart;
        while (!t.plusMinutes(30).isAfter(dayEnd)) {
            times.add(t);
            t = t.plusMinutes(30);
        }
        return times;
    }

    private List<LocalTime> buildBlockEndTimes(Long stylistId, LocalDate date, LocalTime start, Long tenantId) {
        List<StylistSchedule> schedules = stylistScheduleRepository
                .findByStylistIdAndDayAndTenantId(stylistId, date.getDayOfWeek(), tenantId);
        LocalTime dayEnd = schedules.isEmpty()
                ? LocalTime.of(20, 0)
                : schedules.stream().map(StylistSchedule::getEndTime).max(LocalTime::compareTo).orElse(LocalTime.of(20, 0));
        List<LocalTime> times = new ArrayList<>();
        LocalTime t = start.plusMinutes(30);
        while (!t.isAfter(dayEnd)) {
            times.add(t);
            t = t.plusMinutes(30);
        }
        return times;
    }

    // ----- GESTIONAR CITAS (completar / no-show / cancelar) -----

    private void showStylistAppointments(TelegramMessage msg, ConversationState state, Long tenantId) {
        Optional<Stylist> stylistOpt = ensureStylist(msg, tenantId);
        if (stylistOpt.isEmpty()) {
            channel.sendMessage(msg.chatId(), "Este comando es solo para estilistas.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        List<AppointmentResponseDto> appointments;
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            appointments = appointmentService.findAppointmentsByFilters(
                    stylistOpt.get().getId(), null, null, LocalDateTime.now().minusHours(1), null);
        } finally {
            TenantInterceptor.clear();
        }
        List<AppointmentResponseDto> manageable = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .toList();
        if (manageable.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No tenés citas activas para gestionar.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        List<Button> buttons = new ArrayList<>();
        for (AppointmentResponseDto a : manageable) {
            String label = a.getStartDate().format(TIME_FMT) + " "
                    + (a.getService() != null ? a.getService().getName() : "") + " · "
                    + (a.getClient() != null ? a.getClient().getName() : "");
            buttons.add(new Button("✅ " + label, PREFIX_APPT_COMPLETE + a.getId()));
            buttons.add(new Button("🚫 " + label, PREFIX_APPT_NOSHOW + a.getId()));
            buttons.add(new Button("❌ " + label, PREFIX_APPT_CANCEL + a.getId()));
        }
        buttons.add(new Button("◀ Volver", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué querés hacer con cada cita?", buttons);
        updateState(state, STEP_STYLIST_APPT, null);
    }

    private void manageAppointmentByCallback(TelegramMessage msg, ConversationState state, Long tenantId, String prefix, String appointmentIdText) {
        Long appointmentId;
        try {
            appointmentId = Long.parseLong(appointmentIdText);
        } catch (NumberFormatException e) {
            channel.sendMessage(msg.chatId(), "Cita inválida.");
            showMenu(msg, tenantId);
            updateState(state, STEP_MENU, null);
            return;
        }
        boolean ok;
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            if (PREFIX_APPT_COMPLETE.equals(prefix)) {
                ok = appointmentService.completeAppointment(appointmentId);
            } else if (PREFIX_APPT_NOSHOW.equals(prefix)) {
                ok = appointmentService.noShowAppointment(appointmentId);
            } else {
                ok = appointmentService.cancelAppointmentByStylist(appointmentId);
            }
        } catch (Exception e) {
            log.error("Error gestionando cita {} para chat_id={}: {}", appointmentId, msg.chatId(), e.getMessage(), e);
            ok = false;
        } finally {
            TenantInterceptor.clear();
        }
        channel.sendMessage(msg.chatId(), ok
                ? "✅ Listo."
                : "No se pudo actualizar esa cita (¿ya no está activa?).");
        showMenu(msg, tenantId);
        updateState(state, STEP_MENU, null);
    }

    // ============================ FLUJO: RECORDATORIOS (Fase 5) ============================

    private void handleReminderConfirm(TelegramMessage msg, ConversationState state, Long tenantId, String appointmentIdText) {
        Long appointmentId = parseLongId(appointmentIdText);
        if (appointmentId == null) {
            channel.sendMessage(msg.chatId(), "Cita inválida.");
            return;
        }
        boolean ok;
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            ok = appointmentService.confirmAppointment(appointmentId);
        } catch (Exception e) {
            log.error("Error confirmando cita {} para chat_id={}: {}", appointmentId, msg.chatId(), e.getMessage(), e);
            ok = false;
        } finally {
            TenantInterceptor.clear();
        }
        channel.sendMessage(msg.chatId(), ok
                ? "✅ ¡Gracias por confirmar! Te esperamos."
                : "No pudimos confirmar tu cita (¿ya estaba confirmada o cancelada?).");
        showMenu(msg, tenantId);
        updateState(state, STEP_MENU, null);
    }

    private void handleReminderCancel(TelegramMessage msg, ConversationState state, Long tenantId, String appointmentIdText) {
        Long appointmentId = parseLongId(appointmentIdText);
        if (appointmentId == null) {
            channel.sendMessage(msg.chatId(), "Cita inválida.");
            return;
        }
        boolean ok;
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            ok = appointmentService.cancelAppointment(appointmentId);
        } catch (Exception e) {
            log.error("Error cancelando cita {} para chat_id={}: {}", appointmentId, msg.chatId(), e.getMessage(), e);
            ok = false;
        } finally {
            TenantInterceptor.clear();
        }
        channel.sendMessage(msg.chatId(), ok
                ? "❌ Tu cita fue cancelada. Si querés reagendar, usá «Agendar cita»."
                : "No pudimos cancelar tu cita (¿ya está cancelada?).");
        showMenu(msg, tenantId);
        updateState(state, STEP_MENU, null);
    }

    private Long parseLongId(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ============================ HELPERS ============================

    private Long resolveTenant(TelegramMessage msg) {
        String text = msg.text();
        if (text != null && text.startsWith("/start")) {
            String payload = text.substring("/start".length()).trim();
            if (!payload.isEmpty()) {
                return tenantRepository.findByTenantKey(payload)
                        .map(tenant -> tenant.getId())
                        .orElse(null);
            }
        }
        Optional<Client> client = clientRepository.findByTelegramChatId(msg.chatId());
        if (client.isPresent() && client.get().getTenant() != null) {
            return client.get().getTenant().getId();
        }
        return stylistRepository.findByTelegramChatId(msg.chatId())
                .map(stylist -> (stylist.getTenant() != null) ? stylist.getTenant().getId() : null)
                .orElse(null);
    }

    private Client ensureClient(TelegramMessage msg, Long tenantId) {
        return clientRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId)
                .orElseGet(() -> {
                    Client client = Client.builder()
                            .email("tg_" + msg.chatId() + "@bot.local")
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .nameClient(displayName(msg))
                            .phone(null)
                            .telegramChatId(msg.chatId())
                            .tenant(Tenant.builder().id(tenantId).build())
                            .build();
                    return clientRepository.save(client);
                });
    }

    private void showMenu(TelegramMessage msg, Long tenantId) {
        boolean isStylist = tenantId != null && ensureStylist(msg, tenantId).isPresent();
        if (isStylist) {
            channel.sendKeyboard(msg.chatId(), "Hola " + safeName(msg) + "! ¿Qué querés hacer?",
                    List.of("Agendar cita", "Mis citas", "Cancelar cita", "Ver agenda", "Bloquear horario", "Gestionar citas"));
        } else {
            channel.sendKeyboard(msg.chatId(), "Hola " + safeName(msg) + "! ¿Qué querés hacer?",
                    List.of("Agendar cita", "Mis citas", "Cancelar cita"));
        }
    }

    private void sendGuidance(TelegramMessage msg) {
        channel.sendMessage(msg.chatId(),
                "Hola " + safeName(msg) + "! Para empezar, abrí el enlace de tu salón (deep link de Telegram, ej: https://t.me/SU_BOT?start=tenantKey).");
    }

    private void updateState(ConversationState state, String step, Map<String, String> data) {
        state.setCurrentStep(step);
        state.setData(toData(data));
        state.setUpdatedAt(LocalDateTime.now());
        conversationStateService.save(state);
    }

    private Map<String, String> parseData(String data) {
        if (data == null || data.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(data, new TypeReference<HashMap<String, String>>() {
            });
        } catch (Exception e) {
            log.warn("No se pudo parsear data de conversación: {}", data);
            return new HashMap<>();
        }
    }

    private String toData(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {
        }
        return null;
    }

    private String displayName(TelegramMessage msg) {
        if (msg.firstName() != null && !msg.firstName().isBlank()) {
            return msg.firstName();
        }
        if (msg.username() != null && !msg.username().isBlank()) {
            return msg.username();
        }
        return "Cliente Telegram";
    }

    private String safeName(TelegramMessage msg) {
        String name = displayName(msg);
        if (msg.username() != null && !msg.username().isBlank()) {
            return "@" + msg.username();
        }
        return name;
    }
}
