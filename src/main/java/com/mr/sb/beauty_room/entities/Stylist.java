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
    private String name_stylist;

    @Column(nullable = false)
    private String phone;

    @OneToMany(mappedBy = "stylist", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<StylistSchedule> stylistSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "stylist", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Service> services = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "id_stylist_room", nullable = true)
    @JsonBackReference
    private StylistRoom stylistRoom;

    @PrePersist
    public void prePersist() {
        this.setRole(Role.STYLIST);
    }
}

