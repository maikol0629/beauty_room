package com.mr.sb.beauty_room.controllers.panel;

import com.mr.sb.beauty_room.dto.service.SalonServiceResponseDto;
import com.mr.sb.beauty_room.dto.service.SalonServiceSaveDto;
import com.mr.sb.beauty_room.services.ISalonService;
import com.mr.sb.beauty_room.services.IStylistService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/panel/services")
@RequiredArgsConstructor
public class PanelServiceController {

    private final PanelTenantHelper tenantHelper;
    private final ISalonService serviceService;
    private final IStylistService stylistService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("services", tenantHelper.withTenant(serviceService::findAll));
        return "panel/services/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("service", new SalonServiceSaveDto());
        model.addAttribute("serviceId", null);
        model.addAttribute("stylists", tenantHelper.withTenant(stylistService::findAll));
        return "panel/services/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        SalonServiceResponseDto dto = tenantHelper.withTenant(() -> serviceService.findById(id));
        if (dto == null) {
            return "redirect:/panel/services";
        }
        model.addAttribute("service", SalonServiceSaveDto.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .duration(dto.getDuration())
                .stylistId(dto.getStylistId())
                .build());
        model.addAttribute("serviceId", id);
        model.addAttribute("stylists", tenantHelper.withTenant(stylistService::findAll));
        return "panel/services/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("service") SalonServiceSaveDto dto,
                         BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("serviceId", null);
            model.addAttribute("stylists", tenantHelper.withTenant(stylistService::findAll));
            return "panel/services/form";
        }
        try {
            tenantHelper.withTenant(() -> {
                serviceService.save(dto);
                return null;
            });
            ra.addFlashAttribute("success", "Servicio creado correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo crear el servicio: " + e.getMessage());
        }
        return "redirect:/panel/services";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("service") SalonServiceSaveDto dto,
                         BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("serviceId", id);
            model.addAttribute("stylists", tenantHelper.withTenant(stylistService::findAll));
            return "panel/services/form";
        }
        try {
            boolean ok = tenantHelper.withTenant(() -> serviceService.update(dto, id));
            ra.addFlashAttribute(ok ? "success" : "error",
                    ok ? "Servicio actualizado correctamente" : "No se pudo actualizar el servicio");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo actualizar el servicio: " + e.getMessage());
        }
        return "redirect:/panel/services";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes ra) {
        boolean ok = tenantHelper.withTenant(() -> serviceService.deleteById(id));
        ra.addFlashAttribute(ok ? "success" : "error",
                ok ? "Servicio eliminado" : "No se pudo eliminar el servicio");
        return "redirect:/panel/services";
    }
}
