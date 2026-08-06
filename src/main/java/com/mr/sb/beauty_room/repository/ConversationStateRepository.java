package com.mr.sb.beauty_room.repository;

import com.mr.sb.beauty_room.entities.ConversationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationStateRepository extends JpaRepository<ConversationState, Long> {

    Optional<ConversationState> findByChatId(String chatId);
}
