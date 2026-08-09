package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.messaging.Button;
import com.mr.sb.beauty_room.dto.messaging.TemplateMessage;
import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.entities.Notification;
import com.mr.sb.beauty_room.entities.NotificationStatus;
import com.mr.sb.beauty_room.entities.NotificationType;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.NotificationRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.services.IConversationStateService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.mr.sb.beauty_room.services.IReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImplement implements IReminderService {

    public static final String CB_REMINDER_CONFIRM = "REMINDER_CONFIRM";
    public static final String CB_REMINDER_CANCEL = "REMINDER_CANCEL";
    public static final String PREFIX_REMINDER_CONFIRM = CB_REMINDER_CONFIRM + ":";
    public static final String PREFIX_REMINDER_CANCEL = CB_REMINDER_CANCEL + ":";
    public static final String DATA_REMINDER_APPT_ID = "reminderAppointmentId";

    private static final Duration WINDOW_24H = Duration.ofHours(24);
    private static final Duration WINDOW_2H = Duration.ofHours(2);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final TenantRepository tenantRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final StylistRepository stylistRepository;
    private final IConversationStateService conversationStateService;
    private final ConversationStateHelper stateHelper;
    private final IMessagingChannel messagingChannel;

    @Override
    public void sendUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentStatus> active = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
        for (Tenant tenant : tenantRepository.findAll()) {
            if (!tenant.isUsable()) {
                continue;
            }
            List<Appointment> candidates = appointmentRepository.findRemindable(
                    tenant.getId(), active, now, now.plus(WINDOW_24H));
            for (Appointment appointment : candidates) {
                sendReminderIfDue(appointment, now);
            }
        }
    }

    private void sendReminderIfDue(Appointment appointment, LocalDateTime now) {
        Client client = appointment.getClient();
        if (client == null) {
            return;
        }
        Long tenantId = appointment.getTenant() != null ? appointment.getTenant().getId() : null;
        if (tenantId == null) {
            return;
        }
        LocalDateTime start = appointment.getStartDate();
        if (start.isAfter(now) && !start.isAfter(now.plus(WINDOW_2H))) {
            sendReminder(appointment, client, tenantId, NotificationType.REMINDER_2H);
        } else if (start.isAfter(now.plus(WINDOW_2H)) && !start.isAfter(now.plus(WINDOW_24H))) {
            sendReminder(appointment, client, tenantId, NotificationType.REMINDER_24H);
        }
    }

    private void sendReminder(Appointment appointment, Client client, Long tenantId, NotificationType type) {
        if (notificationRepository.existsByAppointmentIdAndType(appointment.getId(), type)) {
            return;
        }
        String serviceName = appointment.getService() != null ? appointment.getService().getNameService() : "cita";
        String date = appointment.getStartDate().format(DATE_FMT);
        String time = appointment.getStartDate().format(TIME_FMT);
        String text = NotificationType.REMINDER_24H.equals(type)
                ? "⏰ Recordatorio: tenés una cita de " + serviceName + " el " + date + " a las " + time + ". ¿Confirmás que vas?"
                : "🕑 Recordatorio: tu cita de " + serviceName + " es hoy a las " + time + ". Te esperamos.";
        String chatId = chatTarget(client);
        if (chatId == null) {
            return;
        }
        persistReminderContext(chatId, appointment.getId());
        TemplateMessage template = new TemplateMessage(
                NotificationType.REMINDER_24H.equals(type) ? "appointment_reminder_24h" : "appointment_reminder_2h",
                "es",
                List.of(serviceName, date, time),
                text,
                List.of(
                        new Button("✅ Confirmar", PREFIX_REMINDER_CONFIRM + appointment.getId()),
                        new Button("❌ Cancelar cita", PREFIX_REMINDER_CANCEL + appointment.getId())));
        messagingChannel.sendTemplate(chatId, template);
        notificationRepository.save(Notification.builder()
                .userId(client.getId())
                .appointmentId(appointment.getId())
                .type(type)
                .subject("Recordatorio de cita")
                .message(text)
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .tenant(Tenant.builder().id(tenantId).build())
                .build());
    }

    /**
     * Guarda el id de la cita del recordatorio en el estado de conversación del
     * chat para resolver los callbacks de template de WhatsApp (que tienen botones
     * de id fijo, sin payload dinámico).
     */
    private void persistReminderContext(String chatId, Long appointmentId) {
        try {
            ConversationState state = conversationStateService.getOrCreate(chatId);
            var data = new java.util.HashMap<>(stateHelper.parseData(state.getData()));
            data.put(DATA_REMINDER_APPT_ID, String.valueOf(appointmentId));
            stateHelper.updateState(state, state.getCurrentStep(), data);
        } catch (Exception e) {
            log.warn("No se pudo persistir el contexto del recordatorio para chat {}: {}", chatId, e.getMessage());
        }
    }

    private String chatTarget(Client client) {
        if (!isBlank(client.getWhatsappChatId())) {
            return client.getWhatsappChatId();
        }
        return isBlank(client.getTelegramChatId()) ? null : client.getTelegramChatId();
    }

    @Override
    public void sendDailySummary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        for (Tenant tenant : tenantRepository.findAll()) {
            if (!tenant.isUsable()) {
                continue;
            }
            List<Stylist> stylists = stylistRepository.findByTenantId(tenant.getId());
            for (Stylist stylist : stylists) {
                sendStylistSummary(stylist, tenant.getId(), startOfDay, endOfDay);
            }
        }
    }

    private void sendStylistSummary(Stylist stylist, Long tenantId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        if (isBlank(stylist.getWhatsappChatId()) && isBlank(stylist.getTelegramChatId())) {
            return;
        }
        if (notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                stylist.getId(), NotificationType.DAILY_SUMMARY, startOfDay)) {
            return;
        }
        List<Appointment> day = appointmentRepository.findStylistDay(stylist.getId(), tenantId, startOfDay, endOfDay);
        StringBuilder sb = new StringBuilder("📋 Resumen del día " + startOfDay.format(DATE_FMT) + ":\n\n");
        if (day.isEmpty()) {
            sb.append("No tenés citas hoy. ¡Que tengas un gran día!");
        } else {
            for (Appointment a : day) {
                sb.append("• ").append(a.getStartDate().format(TIME_FMT))
                        .append(" — ").append(a.getService() != null ? a.getService().getNameService() : "?")
                        .append(" (").append(a.getClient() != null ? a.getClient().getNameClient() : "?")
                        .append(") [").append(a.getStatus()).append("]\n");
            }
        }
        String summary = sb.toString();
        String chatId = isBlank(stylist.getWhatsappChatId()) ? stylist.getTelegramChatId() : stylist.getWhatsappChatId();
        TemplateMessage template = new TemplateMessage(
                "daily_summary", "es", List.of(summary), summary, null);
        messagingChannel.sendTemplate(chatId, template);
        notificationRepository.save(Notification.builder()
                .userId(stylist.getId())
                .type(NotificationType.DAILY_SUMMARY)
                .subject("Resumen diario")
                .message(summary)
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .tenant(Tenant.builder().id(tenantId).build())
                .build());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
