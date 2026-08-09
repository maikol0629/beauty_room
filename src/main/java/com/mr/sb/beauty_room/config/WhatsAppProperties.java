package com.mr.sb.beauty_room.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la integración con WhatsApp (Meta Cloud API).
 * Los valores vacíos desactivan el canal: el bot no envía ni procesa WhatsApp.
 */
@Data
@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    private String accessToken;
    private String phoneNumberId;
    private String phoneNumber;
    private String verifyToken;
    private String appSecret;
    private String apiVersion = "v21.0";
    private String path = "/api/whatsapp/webhook";
    private String webhookUrl;
}
