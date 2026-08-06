package com.mr.sb.beauty_room.Controllers.panel;

import com.mr.sb.beauty_room.DTOS.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.Services.IAppointmentService;
import com.mr.sb.beauty_room.Services.IClientService;
import com.mr.sb.beauty_room.Services.IQrCodeService;
import com.mr.sb.beauty_room.Services.IServiceService;
import com.mr.sb.beauty_room.Services.IStylistService;
import com.mr.sb.beauty_room.entities.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/panel")
@RequiredArgsConstructor
public class PanelController {

    private final PanelTenantHelper tenantHelper;
    private final IQrCodeService qrCodeService;
    private final IServiceService serviceService;
    private final IStylistService stylistService;
    private final IClientService clientService;
    private final IAppointmentService appointmentService;

    @GetMapping("/login")
    public String login() {
        return "panel/login";
    }

    @GetMapping({"", "/"})
    public String home(Model model) {
        int servicesCount = tenantHelper.withTenant(() -> serviceService.findAll().size());
        int stylistsCount = tenantHelper.withTenant(() -> stylistService.findAll().size());
        int clientsCount = tenantHelper.withTenant(() -> clientService.findAll().size());

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);
        List<AppointmentResponseDto> todayAppointments = tenantHelper.withTenant(
                () -> appointmentService.findAppointmentsByFilters(null, null, null, startOfToday, endOfToday));

        model.addAttribute("servicesCount", servicesCount);
        model.addAttribute("stylistsCount", stylistsCount);
        model.addAttribute("clientsCount", clientsCount);
        model.addAttribute("todayAppointments", todayAppointments);
        return "panel/home";
    }

    @GetMapping("/public")
    public String publicAgenda(Model model) {
        Tenant tenant = tenantHelper.currentTenant();
        String publicUrl = qrCodeService.buildPublicAgendaUrl(tenant.getTenantKey());
        model.addAttribute("tenantKey", tenant.getTenantKey());
        model.addAttribute("tenantName", tenant.getName());
        model.addAttribute("publicUrl", publicUrl);
        return "panel/public";
    }

    @GetMapping(value = "/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrImage() {
        Tenant tenant = tenantHelper.currentTenant();
        String publicUrl = qrCodeService.buildPublicAgendaUrl(tenant.getTenantKey());
        if (publicUrl == null) {
            return ResponseEntity.noContent().build();
        }
        byte[] png = qrCodeService.generatePng(publicUrl, 320, 320);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
