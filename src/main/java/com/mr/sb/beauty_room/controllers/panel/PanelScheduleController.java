package com.mr.sb.beauty_room.controllers.panel;

import com.mr.sb.beauty_room.dto.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.services.IStylistScheduleService;
import com.mr.sb.beauty_room.services.IStylistService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.StylistSchedule;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/panel/schedules")
@RequiredArgsConstructor
public class PanelScheduleController {

    private final PanelTenantHelper tenantHelper;
    private final IStylistScheduleService scheduleService;
    private final IStylistService stylistService;

    @GetMapping
    public String list(Model model) {
        List<StylistResponseDto> stylists = tenantHelper.withTenant(stylistService::findAll);
        Map<String, List<StylistSchedule>> schedulesByStylist = new LinkedHashMap<>();
        for (StylistResponseDto stylist : stylists) {
            List<StylistSchedule> schedules = tenantHelper.withTenant(
                    () -> scheduleService.findScheduleByStylistId(stylist.getId()));
            schedulesByStylist.put(stylist.getName(), schedules);
        }
        model.addAttribute("schedulesByStylist", schedulesByStylist);
        return "panel/schedules/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("stylists", tenantHelper.withTenant(stylistService::findAll));
        model.addAttribute("days", DayOfWeek.values());
        return "panel/schedules/form";
    }

    @PostMapping
    public String create(@RequestParam long stylistId,
                         @RequestParam DayOfWeek day,
                         @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
                         @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
                         RedirectAttributes ra) {
        try {
            StylistSchedule schedule = StylistSchedule.builder()
                    .stylist(Stylist.builder().id(stylistId).build())
                    .day(day)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
            tenantHelper.withTenant(() -> scheduleService.saveStylistSchedule(schedule));
            ra.addFlashAttribute("success", "Horario creado correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo crear el horario: " + e.getMessage());
        }
        return "redirect:/panel/schedules";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        boolean ok = tenantHelper.withTenant(() -> scheduleService.deleteStylistSchedule(id));
        ra.addFlashAttribute(ok ? "success" : "error",
                ok ? "Horario eliminado" : "No se pudo eliminar el horario");
        return "redirect:/panel/schedules";
    }
}
