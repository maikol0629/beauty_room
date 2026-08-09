package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Cliente HTTP de la Cloud API de Meta para WhatsApp. Construye la URL del
 * endpoint de mensajes y delega el envío vía RestClient de Spring.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppApiClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppApiClient.class);
    private static final String GRAPH_URL = "https://graph.facebook.com";

    private final WhatsAppProperties properties;
    private final RestClient.Builder restClientBuilder;

    public boolean send(Map<String, Object> payload) {
        String token = properties.getAccessToken();
        String phoneNumberId = properties.getPhoneNumberId();
        if (isBlank(token) || isBlank(phoneNumberId)) {
            log.warn("WhatsApp no configurado (falta access-token/phone-number-id). Mensaje no enviado.");
            return false;
        }
        String url = GRAPH_URL + "/" + properties.getApiVersion() + "/" + phoneNumberId + "/messages";
        try {
            RestClient restClient = restClientBuilder
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            restClient.post().uri(url).body(payload).retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Error enviando mensaje por WhatsApp: {}", e.getMessage());
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
