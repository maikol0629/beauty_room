package com.mr.sb.beauty_room.Services.implement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.sb.beauty_room.DTOS.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.DTOS.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.DTOS.telegram.Button;
import com.mr.sb.beauty_room.DTOS.telegram.TelegramMessage;
import com.mr.sb.beauty_room.Exceptions.AppointmentConflictException;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.Services.IAppointmentService;
import com.mr.sb.beauty_room.Services.IConversationStateService;
import com.mr.sb.beauty_room.Services.IMessagingChannel;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.entities.Service;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.ServiceRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
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

@org.springframework.stereotype.Service
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

    private static final String CB_MENU = "MENU";
    private static final String CB_AGENDAR = "AGENDAR";
    private static final String CB_MIS_CITAS = "MIS_CITAS";
    private static final String CB_CANCELAR_CITA = "CANCELAR_CITA";
    private static final String CB_CONFIRM = "CONFIRM";
    private static final String CB_ABORT = "ABORT";
    private static final String CB_BACK_DATES = "BACK_DATES";
    private static final String PREFIX_SERVICE = "SERVICE:";
    private static final String PREFIX_DATE = "DATE:";
    private static final String PREFIX_TIME = "TIME:";
    private static final String PREFIX_CANCEL_APPT = "CANCEL_APPT:";

    private final IMessagingChannel channel;
    private final IConversationStateService conversationStateService;
    private final IAppointmentService appointmentService;
    private final ClientRepository clientRepository;
    private final TenantRepository tenantRepository;
    private final ServiceRepository serviceRepository;
    private final StylistRepository stylistRepository;
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
                showMenu(msg);
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
        if (text.equalsIgnoreCase("/reschedule")) {
            channel.sendMessage(msg.chatId(), "El reagendado llega pronto. Por ahora, cancelá la cita y agendá una nueva.");
            showMenu(msg);
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
            showMenu(msg);
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
                showMenu(msg);
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
            case CB_CONFIRM -> {
                if (tenantId != null) {
                    confirmAppointment(msg, state, data, tenantId);
                } else {
                    sendGuidance(msg);
                }
            }
            case CB_ABORT -> {
                channel.sendMessage(msg.chatId(), "Listo, lo dejamos acá.");
                showMenu(msg);
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
                } else {
                    channel.sendMessage(msg.chatId(), "Opción desconocida. Abrí el menú:");
                    showMenu(msg);
                    updateState(state, STEP_MENU, null);
                }
            }
        }
    }

    // ============================ FLUJO: AGENDAR ============================

    private void startSchedule(TelegramMessage msg, ConversationState state, Long tenantId) {
        List<Service> services = serviceRepository.findByTenantId(tenantId);
        if (services.isEmpty()) {
            channel.sendMessage(msg.chatId(), "Todavía no hay servicios cargados en tu salón. Probá más tarde.");
            showMenu(msg);
            updateState(state, STEP_MENU, null);
            return;
        }
        List<Button> buttons = services.stream()
                .map(s -> new Button(s.getName_service() + " · $" + s.getPrice() + " (" + s.getDuration() + " min)",
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
        Service service = serviceRepository.findByIdAndTenantId(serviceId, tenantId).orElse(null);
        if (service == null || service.getStylist() == null) {
            channel.sendMessage(msg.chatId(), "Ese servicio ya no está disponible. Elegí otro:");
            startSchedule(msg, state, tenantId);
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("serviceId", String.valueOf(service.getId()));
        data.put("stylistId", String.valueOf(service.getStylist().getId()));
        channel.sendMessage(msg.chatId(), "Perfecto, " + service.getName_service() + ".");
        showDateOptions(msg, data, tenantId);
        updateState(state, STEP_CHOOSE_DATE, data);
    }

    private void showDateOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
        String stylistId = data.get("stylistId");
        String serviceId = data.get("serviceId");
        if (stylistId == null || serviceId == null) {
            channel.sendMessage(msg.chatId(), "Perdimos la selección. Empecemos de nuevo:");
            showMenu(msg);
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
            showMenu(msg);
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
            showMenu(msg);
            updateState(state, STEP_MENU, null);
            return;
        }
        Service service = serviceRepository.findByIdAndTenantId(Long.parseLong(serviceId), tenantId).orElse(null);
        Stylist stylist = stylistRepository.findByIdAndTenantId(Long.parseLong(stylistId), tenantId).orElse(null);
        data.put("time", time.toString());

        String text = "📋 Confirmá tu cita:\n\n"
                + "• Servicio: " + (service != null ? service.getName_service() : serviceId) + "\n"
                + "• Estilista: " + (stylist != null ? stylist.getName_stylist() : stylistId) + "\n"
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
            showMenu(msg);
            updateState(state, STEP_MENU, null);
            return;
        }

        Client client = ensureClient(msg, tenantId);
        AppointmentSaveDto dto = AppointmentSaveDto.builder()
                .startDate(LocalDate.parse(dateStr).atTime(LocalTime.parse(timeStr)))
                .id_client(client.getId())
                .id_stylist(Long.parseLong(stylistId))
                .id_service(Long.parseLong(serviceId))
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
            showMenu(msg);
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
        showMenu(msg);
        updateState(state, STEP_MENU, null);
    }

    // ============================ FLUJO: CANCELAR ============================

    private void startCancel(TelegramMessage msg, ConversationState state, Long tenantId) {
        Optional<Client> clientOpt = clientRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId);
        if (clientOpt.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No tenés citas para cancelar. Agendá una tocando «Agendar cita».");
            showMenu(msg);
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
            showMenu(msg);
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
            showMenu(msg);
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
        showMenu(msg);
        updateState(state, STEP_MENU, null);
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
        return clientRepository.findByTelegramChatId(msg.chatId())
                .map(client -> (client.getTenant() != null) ? client.getTenant().getId() : null)
                .orElse(null);
    }

    private Client ensureClient(TelegramMessage msg, Long tenantId) {
        return clientRepository.findByTelegramChatIdAndTenantId(msg.chatId(), tenantId)
                .orElseGet(() -> {
                    Client client = Client.builder()
                            .email("tg_" + msg.chatId() + "@bot.local")
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .name_client(displayName(msg))
                            .phone(null)
                            .telegram_chat_id(msg.chatId())
                            .tenant(Tenant.builder().id(tenantId).build())
                            .build();
                    return clientRepository.save(client);
                });
    }

    private void showMenu(TelegramMessage msg) {
        channel.sendKeyboard(msg.chatId(), "Hola " + safeName(msg) + "! ¿Qué querés hacer?",
                List.of("Agendar cita", "Mis citas", "Cancelar cita"));
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
