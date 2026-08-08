package com.mr.sb.beauty_room.controllers.panel;

import com.mr.sb.beauty_room.dto.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.services.IAppointmentService;
import com.mr.sb.beauty_room.services.IStylistService;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/panel/appointments")
@RequiredArgsConstructor
public class PanelAppointmentController {

    private final PanelTenantHelper tenantHelper;
    private final IAppointmentService appointmentService;
    private final IStylistService stylistService;

    @GetMapping
    public String list(@RequestParam(required = false) Long stylistId,
                       @RequestParam(required = false) AppointmentStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.plusDays(1).atStartOfDay() : null;

        List<AppointmentResponseDto> appointments = tenantHelper.withTenant(
                () -> appointmentService.findAppointmentsByFilters(stylistId, null, status, fromDt, toDt));

        model.addAttribute("appointments", appointments);
        model.addAttribute("stylists", tenantHelper.withTenant(stylistService::findAll));
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("selectedStylist", stylistId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "panel/appointments/list";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable long id, RedirectAttributes ra) {
        boolean ok = tenantHelper.withTenant(() -> appointmentService.confirmAppointment(id));
        ra.addFlashAttribute(ok ? "success" : "error",
                ok ? "Cita confirmada" : "No se pudo confirmar la cita (solo se confirman citas pendientes)");
        return "redirect:/panel/appointments";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable long id, RedirectAttributes ra) {
        boolean ok = tenantHelper.withTenant(() -> appointmentService.completeAppointment(id));
        ra.addFlashAttribute(ok ? "success" : "error",
                ok ? "Cita completada" : "No se pudo completar la cita");
        return "redirect:/panel/appointments";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable long id, RedirectAttributes ra) {
        boolean ok = tenantHelper.withTenant(() -> appointmentService.cancelAppointmentByStylist(id));
        ra.addFlashAttribute(ok ? "success" : "error",
                ok ? "Cita cancelada" : "No se pudo cancelar la cita");
        return "redirect:/panel/appointments";
    }
}
