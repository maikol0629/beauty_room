package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.services.IConversationStateService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.mr.sb.beauty_room.services.ITelegramAccountService;
import com.mr.sb.beauty_room.services.ITelegramViewService;
import com.mr.sb.beauty_room.util.TelegramDateUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.mr.sb.beauty_room.services.CallbackConstants.CB_ABORT;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_AGENDA_HOY;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_AGENDA_SEMANA;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_AGENDAR;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_BACK_DATES;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_CANCELAR_CITA;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_CONFIRM;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_MENU;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_MIS_CITAS;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_APPT_CANCEL;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_APPT_COMPLETE;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_APPT_NOSHOW;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_BLOCK_DATE;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_BLOCK_END;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_BLOCK_START;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_CANCEL_APPT;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_DATE;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_SERVICE;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_TIME;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_CHOOSE_DATE;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_CHOOSE_TIME;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_INITIAL;
import static com.mr.sb.beauty_room.services.CallbackConstants.STEP_MENU;

@Service
@RequiredArgsConstructor
public class TelegramUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateHandler.class);

    private static final String PREFIX_REMINDER_CONFIRM = ReminderServiceImplement.PREFIX_REMINDER_CONFIRM;
    private static final String PREFIX_REMINDER_CANCEL = ReminderServiceImplement.PREFIX_REMINDER_CANCEL;

    private final IMessagingChannel channel;
    private final IConversationStateService conversationStateService;
    private final ITelegramAccountService accountService;
    private final ITelegramViewService view;
    private final ConversationStateHelper stateHelper;
    private final TelegramBookingFlow bookingFlow;
    private final TelegramStylistFlow stylistFlow;
    private final TelegramAccountFlow accountFlow;

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
        Long tenantId = resolveTenant(msg, state);

        if (text.startsWith("/start")) {
            if (tenantId != null) {
                stateHelper.updateState(state, STEP_MENU, null);
                view.showMenu(msg, tenantId);
            } else {
                view.sendGuidance(msg);
                stateHelper.updateState(state, STEP_INITIAL, null);
            }
            return;
        }

        if (tenantId == null) {
            view.sendGuidance(msg);
            stateHelper.updateState(state, STEP_INITIAL, null);
            return;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        if (text.equalsIgnoreCase("/schedule") || text.equalsIgnoreCase("/agendar") || lower.equals("agendar cita")) {
            bookingFlow.startSchedule(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/miscitas") || lower.equals("mis citas")) {
            accountFlow.showMyAppointments(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/cancel") || text.equalsIgnoreCase("/cancelar") || lower.equals("cancelar cita")) {
            accountFlow.startCancel(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/agenda") || lower.equals("ver agenda")) {
            stylistFlow.showStylistAgendaMenu(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/bloquear") || lower.equals("bloquear") || lower.equals("bloquear horario")) {
            stylistFlow.startBlock(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/gestionar") || text.equalsIgnoreCase("/estado") || lower.equals("gestionar citas")) {
            stylistFlow.showStylistAppointments(msg, state, tenantId);
            return;
        }
        if (text.equalsIgnoreCase("/reschedule")) {
            endWithMenu(msg, state, tenantId, "El reagendado llega pronto. Por ahora, cancelá la cita y agendá una nueva.");
            return;
        }

        Map<String, String> data = stateHelper.parseData(state.getData());
        String step = state.getCurrentStep();
        if (STEP_CHOOSE_DATE.equals(step)) {
            bookingFlow.trySelectDateByText(msg, state, data, tenantId, text);
        } else if (STEP_CHOOSE_TIME.equals(step)) {
            bookingFlow.trySelectTimeByText(msg, state, data, tenantId, text);
        } else {
            endWithMenu(msg, state, tenantId, "Elegí una opción del menú:");
        }
    }

    // ============================ CALLBACK ============================

    private void handleCallback(TelegramMessage msg, ConversationState state) {
        String cb = msg.callbackData();
        if (cb == null) {
            return;
        }
        Long tenantId = resolveTenant(msg, state);
        Map<String, String> data = stateHelper.parseData(state.getData());

        switch (cb) {
            case CB_MENU -> {
                view.showMenu(msg, tenantId);
                stateHelper.updateState(state, STEP_MENU, null);
            }
            case CB_AGENDAR -> {
                if (tenantId == null) {
                    view.sendGuidance(msg);
                } else {
                    bookingFlow.startSchedule(msg, state, tenantId);
                }
            }
            case CB_MIS_CITAS -> {
                if (tenantId == null) {
                    view.sendGuidance(msg);
                } else {
                    accountFlow.showMyAppointments(msg, state, tenantId);
                }
            }
            case CB_CANCELAR_CITA -> {
                if (tenantId == null) {
                    view.sendGuidance(msg);
                } else {
                    accountFlow.startCancel(msg, state, tenantId);
                }
            }
            case CB_BACK_DATES -> {
                if (tenantId == null) {
                    view.sendGuidance(msg);
                } else {
                    view.showDateOptions(msg, data, tenantId);
                }
            }
            case CB_AGENDA_HOY -> {
                if (tenantId == null) {
                    view.sendGuidance(msg);
                } else {
                    stylistFlow.showAgenda(msg, state, tenantId, LocalDate.now(), LocalDate.now());
                }
            }
            case CB_AGENDA_SEMANA -> {
                if (tenantId == null) {
                    view.sendGuidance(msg);
                } else {
                    stylistFlow.showAgenda(msg, state, tenantId, LocalDate.now(), LocalDate.now().plusDays(7));
                }
            }
            case CB_CONFIRM -> {
                if (tenantId != null) {
                    bookingFlow.confirmAppointment(msg, state, data, tenantId);
                } else {
                    view.sendGuidance(msg);
                }
            }
            case CB_ABORT -> endWithMenu(msg, state, tenantId, "Listo, lo dejamos acá.");
            default -> {
                if (tenantId == null) {
                    view.sendGuidance(msg);
                    return;
                }
                if (cb.startsWith(PREFIX_SERVICE)) {
                    handleServiceCallback(msg, state, tenantId, cb.substring(PREFIX_SERVICE.length()));
                } else if (cb.startsWith(PREFIX_DATE)) {
                    handleDateCallback(msg, state, data, tenantId, cb.substring(PREFIX_DATE.length()));
                } else if (cb.startsWith(PREFIX_TIME)) {
                    handleTimeCallback(msg, state, data, tenantId, cb.substring(PREFIX_TIME.length()));
                } else if (cb.startsWith(PREFIX_CANCEL_APPT)) {
                    accountFlow.cancelAppointmentByCallback(msg, state, tenantId, cb.substring(PREFIX_CANCEL_APPT.length()));
                } else if (cb.startsWith(PREFIX_BLOCK_DATE) || cb.startsWith(PREFIX_BLOCK_START) || cb.startsWith(PREFIX_BLOCK_END)) {
                    handleBlockCallbacks(msg, state, data, tenantId, cb);
                } else if (cb.startsWith(PREFIX_APPT_COMPLETE) || cb.startsWith(PREFIX_APPT_NOSHOW) || cb.startsWith(PREFIX_APPT_CANCEL)) {
                    handleAppointmentCallbacks(msg, state, tenantId, cb);
                } else if (cb.startsWith(PREFIX_REMINDER_CONFIRM)) {
                    accountFlow.handleReminderConfirm(msg, state, tenantId, cb.substring(PREFIX_REMINDER_CONFIRM.length()));
                } else if (cb.startsWith(PREFIX_REMINDER_CANCEL)) {
                    accountFlow.handleReminderCancel(msg, state, tenantId, cb.substring(PREFIX_REMINDER_CANCEL.length()));
                } else {
                    endWithMenu(msg, state, tenantId, "Opción desconocida. Abrí el menú:");
                }
            }
        }
    }

    // ============================ DESPACHO DE CALLBACKS ============================

    private void handleServiceCallback(TelegramMessage msg, ConversationState state, Long tenantId, String serviceIdText) {
        bookingFlow.selectService(msg, state, tenantId, serviceIdText);
    }

    private void handleDateCallback(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, String dateText) {
        try {
            bookingFlow.selectDate(msg, state, data, tenantId, LocalDate.parse(dateText));
        } catch (Exception e) {
            channel.sendMessage(msg.chatId(), "Fecha inválida. Elegí otra:");
            view.showDateOptions(msg, data, tenantId);
        }
    }

    private void handleTimeCallback(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, String timeText) {
        try {
            bookingFlow.selectTime(msg, state, data, tenantId, TelegramDateUtils.parseTime(timeText));
        } catch (Exception e) {
            channel.sendMessage(msg.chatId(), "Hora inválida. Elegí otra:");
            view.showTimeOptions(msg, data, tenantId);
        }
    }

    private void handleBlockCallbacks(TelegramMessage msg, ConversationState state, Map<String, String> data, Long tenantId, String cb) {
        if (cb.startsWith(PREFIX_BLOCK_DATE)) {
            try {
                stylistFlow.selectBlockDate(msg, state, data, tenantId, LocalDate.parse(cb.substring(PREFIX_BLOCK_DATE.length())));
            } catch (Exception e) {
                channel.sendMessage(msg.chatId(), "Fecha inválida. Elegí otra:");
                stylistFlow.startBlock(msg, state, tenantId);
            }
        } else if (cb.startsWith(PREFIX_BLOCK_START)) {
            try {
                stylistFlow.selectBlockStart(msg, state, data, tenantId, TelegramDateUtils.parseTime(cb.substring(PREFIX_BLOCK_START.length())));
            } catch (Exception e) {
                channel.sendMessage(msg.chatId(), "Hora inválida. Elegí otra:");
                view.showBlockStartOptions(msg, data, tenantId);
            }
        } else if (cb.startsWith(PREFIX_BLOCK_END)) {
            try {
                stylistFlow.selectBlockEnd(msg, state, data, tenantId, TelegramDateUtils.parseTime(cb.substring(PREFIX_BLOCK_END.length())));
            } catch (Exception e) {
                channel.sendMessage(msg.chatId(), "Hora inválida. Elegí otra:");
                view.showBlockEndOptions(msg, data, tenantId);
            }
        }
    }

    private void handleAppointmentCallbacks(TelegramMessage msg, ConversationState state, Long tenantId, String cb) {
        if (cb.startsWith(PREFIX_APPT_COMPLETE)) {
            stylistFlow.manageAppointmentByCallback(msg, state, tenantId, PREFIX_APPT_COMPLETE, cb.substring(PREFIX_APPT_COMPLETE.length()));
        } else if (cb.startsWith(PREFIX_APPT_NOSHOW)) {
            stylistFlow.manageAppointmentByCallback(msg, state, tenantId, PREFIX_APPT_NOSHOW, cb.substring(PREFIX_APPT_NOSHOW.length()));
        } else if (cb.startsWith(PREFIX_APPT_CANCEL)) {
            stylistFlow.manageAppointmentByCallback(msg, state, tenantId, PREFIX_APPT_CANCEL, cb.substring(PREFIX_APPT_CANCEL.length()));
        }
    }

    // ============================ HELPERS ============================

    private Long resolveTenant(TelegramMessage msg, ConversationState state) {
        Long tenantId = state.getTenantId() != null ? state.getTenantId() : accountService.resolveTenant(msg);
        if (tenantId != null && state.getTenantId() == null) {
            state.setTenantId(tenantId);
        }
        return tenantId;
    }

    private void endWithMenu(TelegramMessage msg, ConversationState state, Long tenantId, String text) {
        view.sendEndWithMenu(msg, tenantId, text);
        stateHelper.updateState(state, STEP_MENU, null);
    }
}
