package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.messaging.Button;
import com.mr.sb.beauty_room.dto.messaging.TemplateMessage;
import com.mr.sb.beauty_room.services.IMessagingChannel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canal de WhatsApp sobre la Cloud API de Meta.
 *
 * Mapas de los métodos de la interfaz:
 * - sendMessage         -> mensaje de texto.
 * - sendKeyboard        -> interactive "list" (menús; máx. 10 filas, se trocea).
 * - sendInlineKeyboard  -> interactive "buttons" si hay <=3 botones; si no, list.
 * - sendTemplate        -> template aprobado de Meta; si no está configurado o
 *                          falla el envío, cae a texto libre (fallback).
 */
@Service
@RequiredArgsConstructor
public class WhatsAppChannel implements IMessagingChannel {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppChannel.class);

    private static final int MAX_BUTTONS_PER_MESSAGE = 3;
    private static final int MAX_LIST_ROWS = 10;
    private static final int BUTTON_TITLE_MAX = 20;
    private static final int LIST_TITLE_MAX = 24;

    private final WhatsAppApiClient apiClient;

    @Override
    public void sendMessage(String chatId, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", chatId);
        body.put("type", "text");
        body.put("text", Map.of("body", text));
        apiClient.send(body);
    }

    @Override
    public void sendKeyboard(String chatId, String text, List<String> buttons) {
        List<Button> wrapped = buttons.stream().map(b -> new Button(b, b)).toList();
        sendListMessages(chatId, text, wrapped);
    }

    @Override
    public void sendInlineKeyboard(String chatId, String text, List<Button> buttons) {
        if (buttons.size() <= MAX_BUTTONS_PER_MESSAGE) {
            sendButtonMessage(chatId, text, buttons);
        } else {
            sendListMessages(chatId, text, buttons);
        }
    }

    @Override
    public void sendTemplate(String chatId, TemplateMessage template) {
        if (template.name() == null || template.name().isBlank()) {
            sendMessage(chatId, template.fallbackText());
            return;
        }
        Map<String, Object> templatePayload = new LinkedHashMap<>();
        templatePayload.put("name", template.name());
        templatePayload.put("language", Map.of("code", template.languageCode() == null ? "es" : template.languageCode()));

        List<Map<String, Object>> components = new ArrayList<>();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "body");
        List<Map<String, Object>> params = new ArrayList<>();
        if (template.parameters() != null) {
            for (String p : template.parameters()) {
                Map<String, Object> param = new LinkedHashMap<>();
                param.put("type", "text");
                param.put("text", p);
                params.add(param);
            }
        }
        body.put("parameters", params);
        components.add(body);

        if (template.buttons() != null && !template.buttons().isEmpty()) {
            List<Map<String, Object>> buttonParams = new ArrayList<>();
            for (Button b : template.buttons()) {
                Map<String, Object> param = new LinkedHashMap<>();
                param.put("type", "text");
                param.put("text", b.callbackData());
                buttonParams.add(param);
            }
            Map<String, Object> buttonComponent = new LinkedHashMap<>();
            buttonComponent.put("type", "button");
            buttonComponent.put("sub_type", "quick_reply");
            buttonComponent.put("index", "0");
            buttonComponent.put("parameters", buttonParams);
            components.add(buttonComponent);
        }

        templatePayload.put("components", components);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", chatId);
        payload.put("type", "template");
        payload.put("template", templatePayload);

        if (!apiClient.send(payload)) {
            sendMessage(chatId, template.fallbackText());
        }
    }

    private void sendButtonMessage(String chatId, String text, List<Button> buttons) {
        List<Map<String, Object>> waButtons = buttons.stream()
                .map(b -> Map.<String, Object>of(
                        "type", "reply",
                        "reply", Map.of("id", b.callbackData(), "title", truncate(b.text(), BUTTON_TITLE_MAX))))
                .toList();
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("buttons", waButtons);
        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "button");
        interactive.put("body", Map.of("text", text));
        interactive.put("action", action);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", chatId);
        payload.put("type", "interactive");
        payload.put("interactive", interactive);
        apiClient.send(payload);
    }

    private void sendListMessages(String chatId, String text, List<Button> buttons) {
        for (int i = 0; i < buttons.size(); i += MAX_LIST_ROWS) {
            List<Button> chunk = buttons.subList(i, Math.min(i + MAX_LIST_ROWS, buttons.size()));
            List<Map<String, Object>> rows = chunk.stream()
                    .map(b -> Map.<String, Object>of(
                            "id", b.callbackData(),
                            "title", truncate(b.text(), LIST_TITLE_MAX)))
                    .toList();
            Map<String, Object> section = Map.of("rows", rows);
            Map<String, Object> action = Map.of("button", "Opciones", "sections", List.of(section));

            Map<String, Object> interactive = new LinkedHashMap<>();
            interactive.put("type", "list");
            interactive.put("body", Map.of("text", text));
            interactive.put("action", action);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", chatId);
            payload.put("type", "interactive");
            payload.put("interactive", interactive);
            apiClient.send(payload);
        }
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
