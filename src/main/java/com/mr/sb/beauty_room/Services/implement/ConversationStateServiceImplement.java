package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.Services.IConversationStateService;
import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.repository.ConversationStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationStateServiceImplement implements IConversationStateService {

    private final ConversationStateRepository conversationStateRepository;

    @Override
    public ConversationState getOrCreate(String chatId) {
        return conversationStateRepository.findByChatId(chatId).orElseGet(() -> {
            ConversationState state = ConversationState.builder()
                    .chatId(chatId)
                    .currentStep("INITIAL")
                    .updatedAt(LocalDateTime.now())
                    .build();
            return conversationStateRepository.save(state);
        });
    }

    @Override
    public Optional<ConversationState> findByChatId(String chatId) {
        return conversationStateRepository.findByChatId(chatId);
    }

    @Override
    public ConversationState save(ConversationState state) {
        return conversationStateRepository.save(state);
    }
}
