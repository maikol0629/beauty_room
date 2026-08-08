package com.mr.sb.beauty_room.controllers.panel;

import com.mr.sb.beauty_room.services.IClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/panel/clients")
@RequiredArgsConstructor
public class PanelClientController {

    private final PanelTenantHelper tenantHelper;
    private final IClientService clientService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("clients", tenantHelper.withTenant(clientService::findAll));
        return "panel/clients/list";
    }
}
