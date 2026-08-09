package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.dto.blockedslot.BlockedSlotRequestDto;
import com.mr.sb.beauty_room.dto.messaging.Button;
import com.mr.sb.beauty_room.dto.messaging.ChannelMessage;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.security.TenantScope;
import com.mr.sb.beauty_room.services.IAppointmentService;
import com.mr.sb.beauty_room.services.IBlockedSlotService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.mr.sb.beauty_room.services.IChatAccountService;
import com.mr.sb.beauty_room.services.IChatViewService;
import com.mr.sb.beauty_room.util.ChatDateUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mr.sb.beauty_room.services.CallbackConstants.CB_AGENDA_HOY;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_AGENDA_SEMANA;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_MENU;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_APPT_CANCEL;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_APPT_COMPLETE;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_APPT_NOSHOW;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_BLOCK_DATE;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_BLOCK_DATE;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_BLOCK_END;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_BLOCK_START;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_MENU;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_STYLIST_APPT;

/**
 * Flujo del estilista en el bot: agenda (día/semana), bloqueo de horarios y
 * gestión de citas (completar / no-show / cancelar).
 */
@Service
@RequiredArgsConstructor
public class StylistFlow {

    private static final Logger log = LoggerFactory.getLogger(StylistFlow.class);

    private final IMessagingChannel channel;
    private final IChatViewService view;
    private final IAppointmentService appointmentService;
    private final IBlockedSlotService blockedSlotService;
    private final IChatAccountService accountService;
    private final ConversationStateHelper stateHelper;

    // ----- AGENDA DEL ESTILISTA -----

    public void showStylistAgendaMenu(ChannelMessage msg, ConversationState state, Long tenantId) {
        if (accountService.findStylistByChat(msg, tenantId).isEmpty()) {
            endWithMenu(msg, state, tenantId, "Este comando es solo para estilistas.");
            return;
        }
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué agenda querés ver?", List.of(
                new Button("📅 Hoy", CB_AGENDA_HOY),
                new Button("🗓 Próximos 7 días", CB_AGENDA_SEMANA),
                new Button("◀ Volver", CB_MENU)));
        stateHelper.updateState(state, STEP_MENU, null);
    }

    public void showAgenda(ChannelMessage msg, ConversationState state, Long tenantId, LocalDate from, LocalDate to) {
        Optional<Stylist> stylistOpt = accountService.findStylistByChat(msg, tenantId);
        if (stylistOpt.isEmpty()) {
            endWithMenu(msg, state, tenantId, "Este comando es solo para estilistas.");
            return;
        }
        List<AppointmentResponseDto> appointments = TenantScope.withTenant(tenantId,
                () -> appointmentService.findAppointmentsByFilters(
                        stylistOpt.get().getId(), null, null, from.atStartOfDay(), to.atTime(LocalTime.MAX)));
        view.showAgendaSummary(msg, appointments);
        view.showMenu(msg, tenantId);
        stateHelper.updateState(state, STEP_MENU, null);
    }

    // ----- BLOQUEAR HORARIO -----

    public void startBlock(ChannelMessage msg, ConversationState state, Long tenantId) {
        Optional<Stylist> stylistOpt = accountService.findStylistByChat(msg, tenantId);
        if (stylistOpt.isEmpty()) {
            endWithMenu(msg, state, tenantId, "Este comando es solo para estilistas.");
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("stylistId", String.valueOf(stylistOpt.get().getId()));
        List<Button> buttons = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.plusDays(i);
            buttons.add(new Button(ChatDateUtils.dayName(d, TextStyle.SHORT) + " " + d,
                    PREFIX_BLOCK_DATE + d));
        }
        buttons.add(new Button("◀ Volver", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué día querés bloquear?", buttons);
        stateHelper.updateState(state, STEP_BLOCK_DATE, data);
    }

    public void selectBlockDate(ChannelMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            channel.sendMessage(msg.chatId(), "Esa fecha ya pasó. Elegí otra:");
            startBlock(msg, state, tenantId);
            return;
        }
        data.put("date", date.toString());
        view.showBlockStartOptions(msg, data, tenantId);
        stateHelper.updateState(state, STEP_BLOCK_START, data);
    }

    public void selectBlockStart(ChannelMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalTime start) {
        LocalDate date = LocalDate.parse(data.get("date"));
        if (date.atTime(start).isBefore(LocalDateTime.now())) {
            channel.sendMessage(msg.chatId(), "Esa hora ya pasó. Elegí otra hora de inicio:");
            view.showBlockStartOptions(msg, data, tenantId);
            return;
        }
        data.put("start", start.toString());
        view.showBlockEndOptions(msg, data, tenantId);
        stateHelper.updateState(state, STEP_BLOCK_END, data);
    }

    public void selectBlockEnd(ChannelMessage msg, ConversationState state, Map<String, String> data, Long tenantId, LocalTime end) {
        Long stylistId = Long.parseLong(data.get("stylistId"));
        LocalDate date = LocalDate.parse(data.get("date"));
        LocalTime start = LocalTime.parse(data.get("start"));
        if (!end.isAfter(start)) {
            channel.sendMessage(msg.chatId(), "La hora de fin debe ser posterior a la de inicio:");
            view.showBlockEndOptions(msg, data, tenantId);
            return;
        }
        BlockedSlotRequestDto dto = BlockedSlotRequestDto.builder()
                .stylistId(stylistId)
                .startDate(date.atTime(start))
                .endDate(date.atTime(end))
                .reason("Bloqueado desde el bot")
                .build();
        try {
            TenantScope.runWithTenant(tenantId, () -> {
                blockedSlotService.create(dto);
                channel.sendMessage(msg.chatId(),
                        "✅ Horario bloqueado: " + date + " de " + start.format(ChatDateUtils.TIME_FMT) + " a " + end.format(ChatDateUtils.TIME_FMT) + ".");
            });
        } catch (Exception e) {
            log.error("Error bloqueando horario para chat_id={}: {}", msg.chatId(), e.getMessage(), e);
            channel.sendMessage(msg.chatId(), "No se pudo bloquear ese horario. Intentá de nuevo.");
        }
        view.showMenu(msg, tenantId);
        stateHelper.updateState(state, STEP_MENU, null);
    }

    // ----- GESTIONAR CITAS (completar / no-show / cancelar) -----

    public void showStylistAppointments(ChannelMessage msg, ConversationState state, Long tenantId) {
        Optional<Stylist> stylistOpt = accountService.findStylistByChat(msg, tenantId);
        if (stylistOpt.isEmpty()) {
            endWithMenu(msg, state, tenantId, "Este comando es solo para estilistas.");
            return;
        }
        List<AppointmentResponseDto> appointments = TenantScope.withTenant(tenantId,
                () -> appointmentService.findAppointmentsByFilters(
                        stylistOpt.get().getId(), null, null, LocalDateTime.now().minusHours(1), null));
        List<AppointmentResponseDto> manageable = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .toList();
        if (manageable.isEmpty()) {
            endWithMenu(msg, state, tenantId, "No tenés citas activas para gestionar.");
            return;
        }
        view.showStylistManagementOptions(msg, manageable);
        stateHelper.updateState(state, STEP_STYLIST_APPT, null);
    }

    public void manageAppointmentByCallback(ChannelMessage msg, ConversationState state, Long tenantId, String prefix, String appointmentIdText) {
        Long appointmentId;
        try {
            appointmentId = Long.parseLong(appointmentIdText);
        } catch (NumberFormatException e) {
            endWithMenu(msg, state, tenantId, "Cita inválida.");
            return;
        }
        boolean ok = TenantScope.withTenant(tenantId, () -> {
            try {
                if (PREFIX_APPT_COMPLETE.equals(prefix)) {
                    return appointmentService.completeAppointment(appointmentId);
                } else if (PREFIX_APPT_NOSHOW.equals(prefix)) {
                    return appointmentService.noShowAppointment(appointmentId);
                } else {
                    return appointmentService.cancelAppointmentByStylist(appointmentId);
                }
            } catch (Exception e) {
                log.error("Error gestionando cita {} para chat_id={}: {}", appointmentId, msg.chatId(), e.getMessage(), e);
                return false;
            }
        });
        endWithMenu(msg, state, tenantId, ok
                ? "✅ Listo."
                : "No se pudo actualizar esa cita (¿ya no está activa?).");
    }

    private void endWithMenu(ChannelMessage msg, ConversationState state, Long tenantId, String text) {
        view.sendEndWithMenu(msg, tenantId, text);
        stateHelper.updateState(state, STEP_MENU, null);
    }
}
