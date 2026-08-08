package com.mr.sb.beauty_room.services.implement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.services.IConversationStateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper de la máquina de estados del bot: persiste el paso actual y los datos
 * de la conversación como JSON en ConversationState.
 */
@Service
@RequiredArgsConstructor
public class ConversationStateHelper {

    private static final Logger log = LoggerFactory.getLogger(ConversationStateHelper.class);

    private final IConversationStateService conversationStateService;
    private final ObjectMapper objectMapper;

    public void updateState(ConversationState state, String step, Map<String, String> data) {
        state.setCurrentStep(step);
        state.setData(toData(data));
        state.setUpdatedAt(LocalDateTime.now());
        conversationStateService.save(state);
    }

    public Map<String, String> parseData(String data) {
        if (data == null || data.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(data, new TypeReference<HashMap<String, String>>() {
            });
        } catch (Exception e) {
            log.warn("No se pudo parsear data de conversación: {}", data, e);
            return new HashMap<>();
        }
    }

    private String toData(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("No se pudo serializar data de conversación: {}", data, e);
            return null;
        }
    }
}
