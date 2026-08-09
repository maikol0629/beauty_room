package com.mr.sb.beauty_room.controllers.panel;

import com.mr.sb.beauty_room.config.TelegramBotProperties;
import com.mr.sb.beauty_room.dto.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.dto.stylist.StylistSaveDto;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/panel/stylists")
@RequiredArgsConstructor
public class PanelStylistController {

    private final PanelTenantHelper tenantHelper;
    private final IStylistService stylistService;
    private final TelegramBotProperties telegramBotProperties;

    @GetMapping
    public String list(Model model) {
        List<StylistResponseDto> stylists = tenantHelper.withTenant(stylistService::findAll);
        model.addAttribute("stylists", stylists);
        model.addAttribute("vincularLinks", buildVincularLinks(stylists));
        return "panel/stylists/list";
    }

    private Map<Long, String> buildVincularLinks(List<StylistResponseDto> stylists) {
        Map<Long, String> links = new HashMap<>();
        String username = telegramBotProperties.getUsername();
        if (username == null || username.isBlank()) {
            return links;
        }
        for (StylistResponseDto st : stylists) {
            String code = tenantHelper.withTenant(() -> stylistService.findVincularCode(st.getId()));
            if (code == null || code.isBlank()) {
                code = tenantHelper.withTenant(() -> stylistService.regenerateVincularCode(st.getId()));
            }
            if (code != null) {
                links.put(st.getId(), "https://t.me/" + username + "?start=vincular-" + code);
            }
        }
        return links;
    }

    @PostMapping("/{id}/regenerate-link")
    public String regenerateLink(@PathVariable long id, RedirectAttributes ra) {
        try {
            String code = tenantHelper.withTenant(() -> stylistService.regenerateVincularCode(id));
            ra.addFlashAttribute(code != null ? "success" : "error",
                    code != null ? "Nuevo enlace de vinculación generado" : "No se pudo generar el enlace");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo generar el enlace: " + e.getMessage());
        }
        return "redirect:/panel/stylists";
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

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("stylist", new StylistSaveDto());
        model.addAttribute("stylistId", null);
        return "panel/stylists/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("stylist") StylistSaveDto dto,
                         BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            bindingResult.rejectValue("password", "error.password", "La contraseña es obligatoria");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("stylistId", null);
            return "panel/stylists/form";
        }
        try {
            boolean ok = tenantHelper.withTenant(() -> stylistService.save(dto));
            ra.addFlashAttribute(ok ? "success" : "error",
                    ok ? "Estilista creado correctamente" : "No se pudo crear el estilista");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo crear el estilista: " + e.getMessage());
        }
        return "redirect:/panel/stylists";
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
