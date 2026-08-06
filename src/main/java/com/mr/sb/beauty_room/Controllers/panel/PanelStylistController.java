package com.mr.sb.beauty_room.Controllers.panel;

import com.mr.sb.beauty_room.DTOS.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist.StylistSaveDto;
import com.mr.sb.beauty_room.Services.IStylistService;
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
@RequestMapping("/panel/stylists")
@RequiredArgsConstructor
public class PanelStylistController {

    private final PanelTenantHelper tenantHelper;
    private final IStylistService stylistService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("stylists", tenantHelper.withTenant(stylistService::findAll));
        return "panel/stylists/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        StylistResponseDto dto = tenantHelper.withTenant(() -> stylistService.findById(id));
        if (dto == null) {
            return "redirect:/panel/stylists";
        }
        model.addAttribute("stylist", StylistSaveDto.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .telegramChatId(dto.getTelegramChatId())
                .build());
        model.addAttribute("stylistId", id);
        return "panel/stylists/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("stylist") StylistSaveDto dto,
                         BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("stylistId", id);
            return "panel/stylists/form";
        }
        try {
            boolean ok = tenantHelper.withTenant(() -> stylistService.update(dto, id));
            ra.addFlashAttribute(ok ? "success" : "error",
                    ok ? "Estilista actualizado correctamente" : "No se pudo actualizar el estilista");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo actualizar el estilista: " + e.getMessage());
        }
        return "redirect:/panel/stylists";
    }
}
