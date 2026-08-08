package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.security.TenantScope;
import com.mr.sb.beauty_room.services.IAppointmentService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.mr.sb.beauty_room.services.ITelegramAccountService;
import com.mr.sb.beauty_room.services.ITelegramViewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_CANCEL_SELECT;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_MENU;

@Service
@RequiredArgsConstructor
public class TelegramAccountFlow {

    private static final Logger log = LoggerFactory.getLogger(TelegramAccountFlow.class);

    private final IMessagingChannel channel;
    private final ITelegramViewService view;
    private final IAppointmentService appointmentService;
    private final ITelegramAccountService accountService;
    private final ConversationStateHelper stateHelper;

    public void showMyAppointments(TelegramMessage msg, ConversationState state, Long tenantId) {
        Optional<Client> clientOpt = accountService.findClientByChat(msg, tenantId);
        if (clientOpt.isEmpty()) {
            endWithMenu(msg, state, tenantId, "Todavía no tenés citas. Agendá una tocando «Agendar cita».");
            return;
        }
        List<AppointmentResponseDto> appointments = TenantScope.withTenant(tenantId,
                () -> appointmentService.findAppointmentsByClientID(clientOpt.get().getId()));
        view.showMyAppointmentsSummary(msg, appointments);
        view.showMenu(msg, tenantId);
        stateHelper.updateState(state, STEP_MENU, null);
    }

    public void startCancel(TelegramMessage msg, ConversationState state, Long tenantId) {
        Optional<Client> clientOpt = accountService.findClientByChat(msg, tenantId);
        if (clientOpt.isEmpty()) {
            endWithMenu(msg, state, tenantId, "No tenés citas para cancelar. Agendá una tocando «Agendar cita».");
            return;
        }
        List<AppointmentResponseDto> appointments = TenantScope.withTenant(tenantId,
                () -> appointmentService.findAppointmentsByClientID(clientOpt.get().getId()));
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentResponseDto> cancellable = appointments.stream()
                .filter(a -> (a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                        && a.getStartDate().isAfter(now))
                .toList();
        if (cancellable.isEmpty()) {
            endWithMenu(msg, state, tenantId, "No tenés citas activas para cancelar.");
            return;
        }
        view.showCancelOptions(msg, cancellable);
        stateHelper.updateState(state, STEP_CANCEL_SELECT, null);
    }

    public void cancelAppointmentByCallback(TelegramMessage msg, ConversationState state, Long tenantId, String appointmentIdText) {
        Long appointmentId;
        try {
            appointmentId = Long.parseLong(appointmentIdText);
        } catch (NumberFormatException e) {
            endWithMenu(msg, state, tenantId, "Cita inválida.");
            return;
        }
        boolean ok = TenantScope.withTenant(tenantId, () -> {
            try {
                return appointmentService.cancelAppointment(appointmentId);
            } catch (Exception e) {
                log.error("Error cancelando cita {} para chat_id={}: {}", appointmentId, msg.chatId(), e.getMessage(), e);
                return false;
            }
        });
        endWithMenu(msg, state, tenantId, ok
                ? "✅ Cita cancelada."
                : "No se pudo cancelar esa cita (¿ya está cancelada o no es tuya?).");
    }

    public void handleReminderConfirm(TelegramMessage msg, ConversationState state, Long tenantId, String appointmentIdText) {
        Long appointmentId = parseLongId(appointmentIdText);
        if (appointmentId == null) {
            channel.sendMessage(msg.chatId(), "Cita inválida.");
            return;
        }
        boolean ok = TenantScope.withTenant(tenantId, () -> {
            try {
                return appointmentService.confirmAppointment(appointmentId);
            } catch (Exception e) {
                log.error("Error confirmando cita {} para chat_id={}: {}", appointmentId, msg.chatId(), e.getMessage(), e);
                return false;
            }
        });
        endWithMenu(msg, state, tenantId, ok
                ? "✅ ¡Gracias por confirmar! Te esperamos."
                : "No pudimos confirmar tu cita (¿ya estaba confirmada o cancelada?).");
    }

    public void handleReminderCancel(TelegramMessage msg, ConversationState state, Long tenantId, String appointmentIdText) {
        Long appointmentId = parseLongId(appointmentIdText);
        if (appointmentId == null) {
            channel.sendMessage(msg.chatId(), "Cita inválida.");
            return;
        }
        boolean ok = TenantScope.withTenant(tenantId, () -> {
            try {
                return appointmentService.cancelAppointment(appointmentId);
            } catch (Exception e) {
                log.error("Error cancelando cita {} para chat_id={}: {}", appointmentId, msg.chatId(), e.getMessage(), e);
                return false;
            }
        });
        endWithMenu(msg, state, tenantId, ok
                ? "❌ Tu cita fue cancelada. Si querés reagendar, usá «Agendar cita»."
                : "No pudimos cancelar tu cita (¿ya está cancelada?).");
    }

    private Long parseLongId(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void endWithMenu(TelegramMessage msg, ConversationState state, Long tenantId, String text) {
        view.sendEndWithMenu(msg, tenantId, text);
        stateHelper.updateState(state, STEP_MENU, null);
    }
}
