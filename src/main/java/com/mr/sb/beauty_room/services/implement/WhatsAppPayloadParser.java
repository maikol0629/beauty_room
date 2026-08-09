package com.mr.sb.beauty_room.services.implement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.sb.beauty_room.dto.messaging.Channel;
import com.mr.sb.beauty_room.dto.messaging.ChannelMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Normaliza el payload JSON del webhook de WhatsApp (Meta Cloud API) a una lista
 * de ChannelMessage. Un webhook puede traer varios mensajes en una misma entrada.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppPayloadParser {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppPayloadParser.class);

    private final ObjectMapper objectMapper;

    public List<ChannelMessage> parse(String body) {
        List<ChannelMessage> messages = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            for (JsonNode entry : root.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    JsonNode value = change.path("value");
                    JsonNode messagesNode = value.path("messages");
                    if (messagesNode.isMissingNode()) {
                        continue;
                    }
                    String profileName = extractProfileName(value);
                    for (JsonNode m : messagesNode) {
                        ChannelMessage parsed = parseMessage(m, profileName);
                        if (parsed != null) {
                            messages.add(parsed);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("No se pudo parsear el payload de WhatsApp: {}", e.getMessage());
        }
        return messages;
    }

    private String extractProfileName(JsonNode value) {
        JsonNode contacts = value.path("contacts");
        if (contacts.isArray() && contacts.size() > 0) {
            JsonNode name = contacts.get(0).path("profile").path("name");
            return name.isMissingNode() || name.isNull() ? null : name.asText();
        }
        return null;
    }

    private ChannelMessage parseMessage(JsonNode message, String profileName) {
        String chatId = message.path("from").asText(null);
        if (chatId == null) {
            return null;
        }
        String type = message.path("type").asText(null);
        if ("text".equals(type)) {
            String text = message.path("text").path("body").asText(null);
            return new ChannelMessage(Channel.WHATSAPP, chatId, text, null, profileName, null, null);
        }
        if ("interactive".equals(type)) {
            JsonNode interactive = message.path("interactive");
            String interactiveType = interactive.path("type").asText(null);
            String callbackData = null;
            if ("button_reply".equals(interactiveType)) {
                callbackData = interactive.path("button_reply").path("id").asText(null);
            } else if ("list_reply".equals(interactiveType)) {
                callbackData = interactive.path("list_reply").path("id").asText(null);
            }
            if (callbackData == null) {
                return null;
            }
            return new ChannelMessage(Channel.WHATSAPP, chatId, null, null, profileName, null, callbackData);
        }
        return null;
    }
}
