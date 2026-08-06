package com.mr.sb.beauty_room.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "conversation_state")
public class ConversationState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true, length = 100)
    private String chatId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "current_step", length = 100)
    private String currentStep;

    @Column(columnDefinition = "TEXT")
    private String data;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
}
