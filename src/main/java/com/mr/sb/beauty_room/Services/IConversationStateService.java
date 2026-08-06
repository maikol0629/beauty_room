package com.mr.sb.beauty_room.Services;

import com.mr.sb.beauty_room.entities.ConversationState;

import java.util.Optional;

public interface IConversationStateService {

    ConversationState getOrCreate(String chatId);
    Optional<ConversationState> findByChatId(String chatId);
    ConversationState save(ConversationState state);
}
