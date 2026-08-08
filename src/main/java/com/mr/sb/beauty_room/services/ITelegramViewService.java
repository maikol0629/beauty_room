package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.dto.telegram.TelegramMessage;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface ITelegramViewService {

    void showMenu(TelegramMessage msg, Long tenantId);

    void sendGuidance(TelegramMessage msg);

    void showServiceSelection(TelegramMessage msg, List<SalonService> services);

    void showDateOptions(TelegramMessage msg, Map<String, String> data, Long tenantId);

    void showTimeOptions(TelegramMessage msg, Map<String, String> data, Long tenantId);

    void showConfirmationKeyboard(TelegramMessage msg, SalonService service, Stylist stylist,
                                  String serviceId, String stylistId, String dateStr, LocalTime time);

    void showMyAppointmentsSummary(TelegramMessage msg, List<AppointmentResponseDto> citas);

    void showAgendaSummary(TelegramMessage msg, List<AppointmentResponseDto> citas);

    void showCancelOptions(TelegramMessage msg, List<AppointmentResponseDto> cancellable);

    void showBlockStartOptions(TelegramMessage msg, Map<String, String> data, Long tenantId);

    void showBlockEndOptions(TelegramMessage msg, Map<String, String> data, Long tenantId);

    void showStylistManagementOptions(TelegramMessage msg, List<AppointmentResponseDto> manageable);

    void sendEndWithMenu(TelegramMessage msg, Long tenantId, String text);
}
