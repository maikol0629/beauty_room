package com.mr.sb.beauty_room.Controllers.panel;

import com.mr.sb.beauty_room.DTOS.blocked_slot.BlockedSlotRequestDto;
import com.mr.sb.beauty_room.DTOS.blocked_slot.BlockedSlotResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.Services.IBlockedSlotService;
import com.mr.sb.beauty_room.Services.IStylistService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/panel/blocked")
@RequiredArgsConstructor
public class PanelBlockedController {

    private final PanelTenantHelper tenantHelper;
    private final IBlockedSlotService blockedSlotService;
    private final IStylistService stylistService;

    @GetMapping
    public String list(Model model) {
        List<StylistResponseDto> stylists = tenantHelper.withTenant(stylistService::findAll);
        Map<Long, String> stylistNames = new HashMap<>();
        List<BlockedSlotResponseDto> blocked = stylists.stream()
                .flatMap(stylist -> {
                    stylistNames.put(stylist.getId(), stylist.getName());
                    return tenantHelper.withTenant(() -> blockedSlotService.findByStylistId(stylist.getId()))
                            .stream();
                })
                .toList();
        model.addAttribute("blocked", blocked);
        model.addAttribute("stylistNames", stylistNames);
        return "panel/blocked/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("stylists", tenantHelper.withTenant(stylistService::findAll));
        return "panel/blocked/form";
    }

    @PostMapping
    public String create(@RequestParam long stylistId,
                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startDate,
                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endDate,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        try {
            BlockedSlotRequestDto request = BlockedSlotRequestDto.builder()
                    .stylistId(stylistId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .reason(reason)
                    .build();
            tenantHelper.withTenant(() -> blockedSlotService.create(request));
            ra.addFlashAttribute("success", "Bloqueo creado correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo crear el bloqueo: " + e.getMessage());
        }
        return "redirect:/panel/blocked";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        boolean ok = tenantHelper.withTenant(() -> blockedSlotService.delete(id));
        ra.addFlashAttribute(ok ? "success" : "error",
                ok ? "Bloqueo eliminado" : "No se pudo eliminar el bloqueo");
        return "redirect:/panel/blocked";
    }
}
