package com.mr.sb.beauty_room.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "tenant")
public class Tenant {

    /**
     * Un tenant es usable si está ACTIVE y su trial no ha vencido.
     * trialEndsAt == null significa que nunca expira (ej. el tenant de plataforma).
     */
    public boolean isUsable() {
        return status == TenantStatus.ACTIVE
                && (trialEndsAt == null || trialEndsAt.isAfter(LocalDateTime.now()));
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String tenantKey;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TenantPlan plan = TenantPlan.TRIAL;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime trialEndsAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.trialEndsAt == null) {
            this.trialEndsAt = LocalDateTime.now().plusDays(14);
        }
    }

    @JsonIgnore
    public Long getTenantId() {
        return id;
    }
}
