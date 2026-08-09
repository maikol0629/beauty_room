package com.mr.sb.beauty_room.controllers.panel;

import com.mr.sb.beauty_room.exceptions.TenantSuspendedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Maneja en el panel (MVC) los tenants suspendidos/vencidos mostrando una página
 * dedicada, en lugar del error genérico. El API REST lo maneja GlobalExceptionHandler.
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
public class PanelExceptionAdvice {

    @ExceptionHandler(TenantSuspendedException.class)
    public String handleTenantSuspended(TenantSuspendedException ex) {
        return "panel/suspended";
    }
}
