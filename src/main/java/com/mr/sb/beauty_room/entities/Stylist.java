package com.mr.sb.beauty_room.entities;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "stylist")
@EqualsAndHashCode(callSuper = true)
public class Stylist extends User {
    @Column(nullable = false)
    private String nameStylist;

    private String phone;

    private String telegramChatId;

    private String whatsappChatId;

    /** Código secreto del deep link de vinculación (t.me/<bot>?start=vincular-<código>). */
    @Column(name = "vincular_code", unique = true, length = 32)
    @JsonIgnore
    private String vincularCode;

    @OneToMany(mappedBy = "stylist", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<StylistSchedule> stylistSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "stylist", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<SalonService> services = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "id_stylist_room", nullable = true)
    @JsonBackReference
    private StylistRoom stylistRoom;

    @PrePersist
    public void prePersist() {
        if (this.getRole() == null) {
            this.setRole(Role.STYLIST);
        }
    }
}

