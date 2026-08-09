package com.mr.sb.beauty_room.controllers.panel;

import com.mr.sb.beauty_room.dto.superadmin.TenantCreateDto;
import com.mr.sb.beauty_room.dto.superadmin.TenantUpdateDto;
import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.TenantPlan;
import com.mr.sb.beauty_room.entities.TenantStatus;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.services.ISuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/panel/super")
@RequiredArgsConstructor
public class PanelSuperAdminController {

    private final ISuperAdminService superAdminService;
    private final AppointmentRepository appointmentRepository;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalTenants", superAdminService.countTenants());
        model.addAttribute("activeTenants", superAdminService.countTenantsByStatus(TenantStatus.ACTIVE));
        model.addAttribute("suspendedTenants", superAdminService.countTenantsByStatus(TenantStatus.SUSPENDED));
        model.addAttribute("trialTenants", superAdminService.countTenantsByPlan(TenantPlan.TRIAL));
        model.addAttribute("totalClients", superAdminService.countClients());
        model.addAttribute("totalStylists", superAdminService.countStylists());
        model.addAttribute("totalAppointments", superAdminService.countAppointments());
        return "panel/super/dashboard";
    }

    @GetMapping("/tenants")
    public String list(@RequestParam(required = false) TenantPlan plan,
                       @RequestParam(required = false) TenantStatus status,
                       Model model) {
        List<Tenant> tenants = superAdminService.findAllTenants();
        if (plan != null) {
            tenants = tenants.stream().filter(t -> t.getPlan() == plan).toList();
        }
        if (status != null) {
            tenants = tenants.stream().filter(t -> t.getStatus() == status).toList();
        } else {
            tenants = tenants.stream().filter(t -> t.getStatus() != TenantStatus.CANCELLED).toList();
        }
        model.addAttribute("tenants", tenants);
        model.addAttribute("plans", TenantPlan.values());
        model.addAttribute("statuses", TenantStatus.values());
        model.addAttribute("selectedPlan", plan);
        model.addAttribute("selectedStatus", status);
        return "panel/super/tenants";
    }

    @GetMapping("/tenants/new")
    public String newForm(Model model) {
        model.addAttribute("tenant", new TenantCreateDto());
        model.addAttribute("plans", TenantPlan.values());
        model.addAttribute("statuses", TenantStatus.values());
        return "panel/super/tenant-form";
    }

    @PostMapping("/tenants")
    public String create(@Valid @ModelAttribute("tenant") TenantCreateDto dto,
                         BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("plans", TenantPlan.values());
            model.addAttribute("statuses", TenantStatus.values());
            return "panel/super/tenant-form";
        }
        try {
            Tenant created = superAdminService.createTenant(dto);
            ra.addFlashAttribute("success",
                    "Salón \"" + created.getName() + "\" creado (tenantKey: " + created.getTenantKey() + ")");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo crear el salón: " + e.getMessage());
        }
        return "redirect:/panel/super/tenants";
    }

    @GetMapping("/tenants/{id}")
    public String detail(@PathVariable long id, Model model) {
        Tenant tenant = superAdminService.findTenantById(id);
        if (tenant == null) {
            return "redirect:/panel/super/tenants";
        }
        model.addAttribute("tenant", tenant);
        model.addAttribute("users", superAdminService.findTenantUsers(id));
        List<Appointment> appointments = appointmentRepository.findByTenantId(id);
        appointments.sort((a, b) -> b.getStartDate().compareTo(a.getStartDate()));
        model.addAttribute("appointments", appointments.size() > 20 ? appointments.subList(0, 20) : appointments);
        return "panel/super/tenant-detail";
    }

    @GetMapping("/tenants/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        Tenant tenant = superAdminService.findTenantById(id);
        if (tenant == null) {
            return "redirect:/panel/super/tenants";
        }
        model.addAttribute("tenant", TenantUpdateDto.builder()
                .name(tenant.getName())
                .plan(tenant.getPlan())
                .status(tenant.getStatus())
                .trialEndsAt(tenant.getTrialEndsAt())
                .build());
        model.addAttribute("tenantId", id);
        model.addAttribute("plans", TenantPlan.values());
        model.addAttribute("statuses", TenantStatus.values());
        return "panel/super/tenant-edit";
    }

    @PostMapping("/tenants/{id}")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("tenant") TenantUpdateDto dto,
                         BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tenantId", id);
            model.addAttribute("plans", TenantPlan.values());
            model.addAttribute("statuses", TenantStatus.values());
            return "panel/super/tenant-edit";
        }
        try {
            Tenant updated = superAdminService.updateTenant(id, dto);
            if (updated == null) {
                ra.addFlashAttribute("error", "El salón no existe");
                return "redirect:/panel/super/tenants";
            }
            ra.addFlashAttribute("success", "Salón actualizado correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo actualizar el salón: " + e.getMessage());
        }
        return "redirect:/panel/super/tenants/" + id;
    }

    @PostMapping("/tenants/{id}/status")
    public String setStatus(@PathVariable long id, @RequestParam TenantStatus status, RedirectAttributes ra) {
        boolean ok = superAdminService.setTenantStatus(id, status) != null;
        ra.addFlashAttribute(ok ? "success" : "error",
                ok ? "Estado del salón actualizado a " + status : "No se pudo actualizar el estado del salón");
        return "redirect:/panel/super/tenants";
    }

    @PostMapping("/tenants/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes ra) {
        boolean ok = superAdminService.deleteTenant(id);
        ra.addFlashAttribute(ok ? "success" : "error",
                ok ? "Salón eliminado (quedó inaccesible y oculto del listado)" : "No se pudo eliminar el salón");
        return "redirect:/panel/super/tenants";
    }
}
