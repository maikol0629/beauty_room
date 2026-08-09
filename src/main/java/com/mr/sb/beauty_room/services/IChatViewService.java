package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.dto.messaging.ChannelMessage;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Presentación del bot de Telegram: construye teclados y mensajes al usuario.
 */
public interface IChatViewService {

    void showMenu(ChannelMessage msg, Long tenantId);

    void sendGuidance(ChannelMessage msg);

    void showServiceSelection(ChannelMessage msg, List<SalonService> services);

    void showDateOptions(ChannelMessage msg, Map<String, String> data, Long tenantId);

    void showTimeOptions(ChannelMessage msg, Map<String, String> data, Long tenantId);

    void showConfirmationKeyboard(ChannelMessage msg, SalonService service, Stylist stylist,
                                  String serviceId, String stylistId, String dateStr, LocalTime time);

    void showMyAppointmentsSummary(ChannelMessage msg, List<AppointmentResponseDto> citas);

    void showAgendaSummary(ChannelMessage msg, List<AppointmentResponseDto> citas);

    void showCancelOptions(ChannelMessage msg, List<AppointmentResponseDto> cancellable);

    void showBlockStartOptions(ChannelMessage msg, Map<String, String> data, Long tenantId);

    void showBlockEndOptions(ChannelMessage msg, Map<String, String> data, Long tenantId);

    void showStylistManagementOptions(ChannelMessage msg, List<AppointmentResponseDto> manageable);

    void sendEndWithMenu(ChannelMessage msg, Long tenantId, String text);
}
