package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.dto.telegram.Button;
import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.StylistSchedule;
import com.mr.sb.beauty_room.repository.StylistScheduleRepository;
import com.mr.sb.beauty_room.services.IAppointmentService;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import com.mr.sb.beauty_room.services.ITelegramAccountService;
import com.mr.sb.beauty_room.services.ITelegramViewService;
import com.mr.sb.beauty_room.util.TelegramDateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mr.sb.beauty_room.services.CallbackConstants.CB_ABORT;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_BACK_DATES;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_CONFIRM;
import static com.mr.sb.beauty_room.services.CallbackConstants.CB_MENU;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_APPT_CANCEL;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_APPT_COMPLETE;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_APPT_NOSHOW;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_BLOCK_END;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_BLOCK_START;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_CANCEL_APPT;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_DATE;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_SERVICE;
import static com.mr.sb.beauty_room.services.CallbackConstants.PREFIX_TIME;

/**
 * Capa de presentación del bot: construye los teclados y textos que se envían
 * por IMessagingChannel, sin lógica de negocio.
 */
@Service
@RequiredArgsConstructor
public class TelegramViewServiceImplement implements ITelegramViewService {

    private final IMessagingChannel channel;
    private final ITelegramAccountService accountService;
    private final IAppointmentService appointmentService;
    private final StylistScheduleRepository stylistScheduleRepository;

    @Override
    public void showMenu(TelegramMessage msg, Long tenantId) {
        boolean isStylist = tenantId != null && accountService.findStylistByChat(msg, tenantId).isPresent();
        if (isStylist) {
            channel.sendKeyboard(msg.chatId(), "Hola " + accountService.safeName(msg) + "! ¿Qué querés hacer?",
                    List.of("Agendar cita", "Mis citas", "Cancelar cita", "Ver agenda", "Bloquear horario", "Gestionar citas"));
        } else {
            channel.sendKeyboard(msg.chatId(), "Hola " + accountService.safeName(msg) + "! ¿Qué querés hacer?",
                    List.of("Agendar cita", "Mis citas", "Cancelar cita"));
        }
    }

    @Override
    public void sendGuidance(TelegramMessage msg) {
        channel.sendMessage(msg.chatId(),
                "Hola " + accountService.safeName(msg) + "! Para empezar, abrí el enlace de tu salón (deep link de Telegram, ej: https://t.me/SU_BOT?start=tenantKey).");
    }

    @Override
    public void showServiceSelection(TelegramMessage msg, List<SalonService> services) {
        List<Button> buttons = services.stream()
                .map(s -> new Button(s.getNameService() + " · $" + s.getPrice() + " (" + s.getDuration() + " min)",
                        PREFIX_SERVICE + s.getId()))
                .toList();
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué servicio querés agendar?", buttons);
    }

    @Override
    public void showDateOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
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
                .map(d -> new Button(TelegramDateUtils.dayName(d, TextStyle.SHORT) + " " + d,
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

    @Override
    public void showTimeOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
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
                .map(t -> new Button(t.format(TelegramDateUtils.TIME_FMT), PREFIX_TIME + t))
                .toList();
        List<Button> all = new ArrayList<>(buttons);
        all.add(new Button("◀ Otra fecha", CB_BACK_DATES));
        channel.sendInlineKeyboard(msg.chatId(), "Elegí una hora:", all);
    }

    @Override
    public void showConfirmationKeyboard(TelegramMessage msg, SalonService service, Stylist stylist,
                                         String serviceId, String stylistId, String dateStr, LocalTime time) {
        String text = "📋 Confirmá tu cita:\n\n"
                + "• Servicio: " + (service != null ? service.getNameService() : serviceId) + "\n"
                + "• Estilista: " + (stylist != null ? stylist.getNameStylist() : stylistId) + "\n"
                + "• Fecha: " + dateStr + "\n"
                + "• Hora: " + time.format(TelegramDateUtils.TIME_FMT) + "\n\n"
                + "¿Todo correcto?";
        channel.sendInlineKeyboard(msg.chatId(), text, List.of(
                new Button("✅ Confirmar", CB_CONFIRM),
                new Button("❌ Cancelar", CB_ABORT)));
    }

    @Override
    public void showMyAppointmentsSummary(TelegramMessage msg, List<AppointmentResponseDto> citas) {
        if (citas.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No tenés citas registradas.");
            return;
        }
        List<AppointmentResponseDto> sorted = citas.stream()
                .sorted((a, b) -> b.getStartDate().compareTo(a.getStartDate()))
                .toList();
        StringBuilder sb = new StringBuilder("📅 Tus citas:\n\n");
        for (AppointmentResponseDto a : sorted) {
            sb.append("• ").append(a.getStartDate().format(TelegramDateUtils.DATETIME_FMT))
                    .append(" — ").append(a.getService().getName())
                    .append(" (").append(a.getStatus()).append(")\n");
        }
        channel.sendMessage(msg.chatId(), sb.toString());
    }

    @Override
    public void showAgendaSummary(TelegramMessage msg, List<AppointmentResponseDto> citas) {
        List<AppointmentResponseDto> filtered = citas.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.REJECTED)
                .sorted((a, b) -> a.getStartDate().compareTo(b.getStartDate()))
                .toList();
        if (filtered.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No tenés citas en ese período.");
            return;
        }
        StringBuilder sb = new StringBuilder("📋 Tu agenda:\n\n");
        for (AppointmentResponseDto a : filtered) {
            sb.append("• ").append(a.getStartDate().format(TelegramDateUtils.DATETIME_FMT))
                    .append(" — ").append(a.getService() != null ? a.getService().getName() : "?")
                    .append(" (").append(a.getClient() != null ? a.getClient().getName() : "?")
                    .append(") [").append(a.getStatus()).append("]\n");
        }
        channel.sendMessage(msg.chatId(), sb.toString());
    }

    @Override
    public void showCancelOptions(TelegramMessage msg, List<AppointmentResponseDto> cancellable) {
        List<Button> buttons = cancellable.stream()
                .map(a -> new Button("Cancelar " + a.getStartDate().format(TelegramDateUtils.DATETIME_FMT) + " (" + a.getService().getName() + ")",
                        PREFIX_CANCEL_APPT + a.getId()))
                .toList();
        List<Button> all = new ArrayList<>(buttons);
        all.add(new Button("◀ Volver", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué cita querés cancelar?", all);
    }

    @Override
    public void showBlockStartOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
        List<LocalTime> times = buildBlockTimes(Long.parseLong(data.get("stylistId")), LocalDate.parse(data.get("date")), tenantId);
        if (times.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No hay horarios para bloquear ese día. Elegí otro:");
            return;
        }
        List<Button> buttons = new ArrayList<>(times.stream()
                .map(t -> new Button(t.format(TelegramDateUtils.TIME_FMT), PREFIX_BLOCK_START + t))
                .toList());
        buttons.add(new Button("◀ Otro día", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Desde qué hora?", buttons);
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

    @Override
    public void showBlockEndOptions(TelegramMessage msg, Map<String, String> data, Long tenantId) {
        List<LocalTime> times = buildBlockEndTimes(
                Long.parseLong(data.get("stylistId")), LocalDate.parse(data.get("date")),
                LocalTime.parse(data.get("start")), tenantId);
        if (times.isEmpty()) {
            channel.sendMessage(msg.chatId(), "No hay horas de fin disponibles desde esa hora. Elegí otro inicio:");
            showBlockStartOptions(msg, data, tenantId);
            return;
        }
        List<Button> buttons = new ArrayList<>(times.stream()
                .map(t -> new Button(t.format(TelegramDateUtils.TIME_FMT), PREFIX_BLOCK_END + t))
                .toList());
        buttons.add(new Button("◀ Cambiar inicio", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Hasta qué hora?", buttons);
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

    @Override
    public void showStylistManagementOptions(TelegramMessage msg, List<AppointmentResponseDto> manageable) {
        List<Button> buttons = new ArrayList<>();
        for (AppointmentResponseDto a : manageable) {
            String label = a.getStartDate().format(TelegramDateUtils.TIME_FMT) + " "
                    + (a.getService() != null ? a.getService().getName() : "") + " · "
                    + (a.getClient() != null ? a.getClient().getName() : "");
            buttons.add(new Button("✅ " + label, PREFIX_APPT_COMPLETE + a.getId()));
            buttons.add(new Button("🚫 " + label, PREFIX_APPT_NOSHOW + a.getId()));
            buttons.add(new Button("❌ " + label, PREFIX_APPT_CANCEL + a.getId()));
        }
        buttons.add(new Button("◀ Volver", CB_MENU));
        channel.sendInlineKeyboard(msg.chatId(), "¿Qué querés hacer con cada cita?", buttons);
    }

    @Override
    public void sendEndWithMenu(TelegramMessage msg, Long tenantId, String text) {
        channel.sendMessage(msg.chatId(), text);
        showMenu(msg, tenantId);
    }
}
