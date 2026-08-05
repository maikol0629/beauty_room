package com.mr.sb.beauty_room.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client")
@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Client extends User {
    private String name_client;
    private String phone;

    @Column(name = "telegram_chat_id")
    private String telegram_chat_id;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.setRole(Role.CLIENT);
    }
}
