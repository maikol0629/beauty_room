package com.mr.sb.beauty_room.controllers.panel;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expone a todos los templates del panel el atributo isSuperAdmin,
 * para que la navbar muestre el menú correcto según el rol.
 */
@ControllerAdvice(assignableTypes = {
        PanelController.class,
        PanelClientController.class,
        PanelStylistController.class,
        PanelServiceController.class,
        PanelScheduleController.class,
        PanelBlockedController.class,
        PanelAppointmentController.class,
        PanelSuperAdminController.class
})
@RequiredArgsConstructor
public class PanelModelAdvice {

    private final PanelTenantHelper tenantHelper;

    @ModelAttribute("isSuperAdmin")
    public boolean isSuperAdmin() {
        return tenantHelper.isSuperAdmin();
    }
}
