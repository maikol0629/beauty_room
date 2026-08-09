package com.mr.sb.beauty_room.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la cuenta de super administrador de la plataforma.
 * El usuario se crea (si no existe) al arrancar la app.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.superadmin")
public class SuperAdminProperties {

    private String email = "superadmin@beautyroom.app";
    private String password = "superadmin123";
}
