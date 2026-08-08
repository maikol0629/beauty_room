package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import com.mr.sb.beauty_room.exceptions.AppointmentConflictException;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.repository.SalonServiceRepository;
import com.mr.sb.beauty_room.security.TenantScope;
import com.mr.sb.beauty_room.services.IAppointmentService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.mr.sb.beauty_room.services.ITelegramAccountService;
import com.mr.sb.beauty_room.services.ITelegramViewService;
import com.mr.sb.beauty_room.util.TelegramDateUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_CHOOSE_DATE;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_CHOOSE_SERVICE;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_CHOOSE_TIME;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_CONFIRM;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_MENU;

/**
 * Flujo de agendamiento del bot: selección de servicio, fecha, hora y confirmación.
 */
@Service
@RequiredArgsConstructor
public class TelegramBookingFlow {

    private static final Logger log = LoggerFactory.getLogger(TelegramBookingFlow.class);

    private final IMessagingChannel channel;
    private final ITelegramViewService view;
    private final IAppointmentService appointmentService;
    private final ITelegramAccountService accountService;
    private final SalonServiceRepository serviceRepository;
    private final ConversationStateHelper stateHelper;

    public void startSchedule(TelegramMessage msg, ConversationState state, Long tenantId) {
        List<SalonService> services = serviceRepository.findByTenantId(tenantId);
        if (services.isEmpty()) {
            endWithMenu(msg, state, tenantId, "Todavía no hay servicios cargados en tu salón. Probá más tarde.");
            return;
        }
        view.showServiceSelection(msg, services);
        stateHelper.updateState(state, STEP_CHOOSE_SERVICE, new HashMap<>());
    }

    public void selectService(TelegramMessage msg, ConversationState state, Long tenantId, String serviceIdText) {
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
        view.showDateOptions(msg, data, tenantId);
        stateHelper.updateState(state, STEP_CHOOSE_DATE, data);
    }

    public void showDateOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
        view.showDateOptions(msg, data, tenantId);
    }

    public void showTimeOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
        view.showTimeOptions(msg, data, tenantId);
    }

    public void trySelectDateByText(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, String text) {
        LocalDate date = TelegramDateUtils.parseDate(text);
        if (date == null) {
            channel.sendMessage(msg.chatId(), "No entendí la fecha. Usá el formato YYYY-MM-DD (ej: 2026-08-10) o elegí una de las fechas de abajo.");
            view.showDateOptions(msg, data, tenantId);
            return;
        }
        selectDate(msg, state, data, tenantId, date);
    }

    public void trySelectTimeByText(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, String text) {
        LocalTime time;
        try {
            time = TelegramDateUtils.parseTime(text);
        } catch (DateTimeParseException e) {
            channel.sendMessage(msg.chatId(), "No entendí la hora. Usá el formato HH:mm (ej: 10:30) o elegí una de las horas de abajo.");
            view.showTimeOptions(msg, data, tenantId);
            return;
        }
        selectTime(msg, state, data, tenantId, time);
    }

    public void selectDate(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            channel.sendMessage(msg.chatId(), "Esa fecha ya pasó. Elegí otra:");
            view.showDateOptions(msg, data, tenantId);
            return;
        }
        String stylistId = data.get("stylistId");
        String serviceId = data.get("serviceId");
        if (stylistId == null || serviceId == null) {
            endWithMenu(msg, state, tenantId, "Perdimos la selección. Empecemos de nuevo:");
            return;
        }
        List<LocalTime> slots = appointmentService.getAvailableSlots(Long.parseLong(stylistId), Long.parseLong(serviceId), date);
        if (slots.isEmpty()) {
            channel.sendMessage(msg.chatId(), "Sin horarios disponibles el " + date + ". Elegí otra fecha:");
            view.showDateOptions(msg, data, tenantId);
            return;
        }
        data.put("date", date.toString());
        channel.sendMessage(msg.chatId(), "Horarios disponibles el " + TelegramDateUtils.dayName(date, TextStyle.FULL) + " " + date + ":");
        view.showTimeOptions(msg, data, tenantId);
        stateHelper.updateState(state, STEP_CHOOSE_TIME, data);
    }

    public void selectTime(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalTime time) {
        String serviceId = data.get("serviceId");
        String stylistId = data.get("stylistId");
        String dateStr = data.get("date");
        if (serviceId == null || stylistId == null || dateStr == null) {
            endWithMenu(msg, state, tenantId, "Perdimos la selección. Empecemos de nuevo:");
            return;
        }
        SalonService service = serviceRepository.findByIdAndTenantId(Long.parseLong(serviceId), tenantId).orElse(null);
        Stylist stylist = accountService.findStylistByIdAndTenant(Long.parseLong(stylistId), tenantId).orElse(null);
        data.put("time", time.toString());

        view.showConfirmationKeyboard(msg, service, stylist, serviceId, stylistId, dateStr, time);
        stateHelper.updateState(state, STEP_CONFIRM, data);
    }

    public void confirmAppointment(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId) {
        String serviceId = data.get("serviceId");
        String stylistId = data.get("stylistId");
        String dateStr = data.get("date");
        String timeStr = data.get("time");
        if (serviceId == null || stylistId == null || dateStr == null || timeStr == null) {
            endWithMenu(msg, state, tenantId, "Perdimos el hilo de la conversación. Empecemos de nuevo:");
            return;
        }

        Client client = accountService.ensureClient(msg, tenantId);
        AppointmentSaveDto dto = AppointmentSaveDto.builder()
                .startDate(LocalDate.parse(dateStr).atTime(LocalTime.parse(timeStr)))
                .clientId(client.getId())
                .stylistId(Long.parseLong(stylistId))
                .serviceId(Long.parseLong(serviceId))
                .build();

        try {
            TenantScope.runWithTenant(tenantId, () -> {
                boolean ok = appointmentService.save(dto);
                if (ok) {
                    channel.sendMessage(msg.chatId(),
                            "✅ ¡Listo! Tu cita quedó agendada:\n\n"
                                    + "• Fecha: " + TelegramDateUtils.formatDateForUser(LocalDate.parse(dateStr)) + "\n"
                                    + "• Hora: " + timeStr + "\n\n"
                                    + "Te esperamos.");
                    stateHelper.updateState(state, STEP_MENU, null);
                } else {
                    channel.sendMessage(msg.chatId(), "No se pudo agendar la cita. Elegí otra hora:");
                    view.showTimeOptions(msg, data, tenantId);
                    stateHelper.updateState(state, STEP_CHOOSE_TIME, data);
                }
            });
        } catch (AppointmentConflictException e) {
            log.info("Conflicto de agenda para chat_id={}: {}", msg.chatId(), e.getMessage());
            channel.sendMessage(msg.chatId(), "⚠️ Ese horario ya no está disponible. Elegí otra hora:");
            view.showTimeOptions(msg, data, tenantId);
            stateHelper.updateState(state, STEP_CHOOSE_TIME, data);
        } catch (Exception e) {
            log.error("Error agendando cita para chat_id={}: {}", msg.chatId(), e.getMessage(), e);
            channel.sendMessage(msg.chatId(), "Ocurrió un error al agendar. Intentá de nuevo.");
            stateHelper.updateState(state, STEP_MENU, null);
        }
    }

    private void endWithMenu(TelegramMessage msg, ConversationState state, Long tenantId, String text) {
        view.sendEndWithMenu(msg, tenantId, text);
        stateHelper.updateState(state, STEP_MENU, null);
    }
}
