package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.entities.ConversationState;
import com.mr.sb.beauty_room.repository.ConversationStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationStateServiceImplementTest {

    @Mock
    private ConversationStateRepository conversationStateRepository;

    @InjectMocks
    private ConversationStateServiceImplement service;

    @Test
    void getOrCreate_whenAbsent_shouldCreateNewStateWithInitialStep() {
        when(conversationStateRepository.findByChatId("1")).thenReturn(Optional.empty());
        when(conversationStateRepository.save(any(ConversationState.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConversationState state = service.getOrCreate("1");

        assertThat(state.getChatId()).isEqualTo("1");
        assertThat(state.getCurrentStep()).isEqualTo("INITIAL");
        verify(conversationStateRepository).save(any(ConversationState.class));
    }

    @Test
    void getOrCreate_whenPresent_shouldReturnExistingWithoutSaving() {
        ConversationState existing = ConversationState.builder().chatId("1").currentStep("START").build();
        when(conversationStateRepository.findByChatId("1")).thenReturn(Optional.of(existing));

        ConversationState state = service.getOrCreate("1");

        assertThat(state).isSameAs(existing);
        verify(conversationStateRepository, never()).save(any());
    }

    @Test
    void save_shouldDelegateToRepository() {
        ConversationState state = ConversationState.builder().chatId("2").build();
        when(conversationStateRepository.save(state)).thenReturn(state);

        assertThat(service.save(state)).isSameAs(state);
        verify(conversationStateRepository).save(state);
    }
}
