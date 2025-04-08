package com.mr.sb.beauty_room.entities;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "stylist_room")
public class StylistRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String name_room;
    @Column(nullable = false)
    private String address;

    @OneToMany(mappedBy = "stylistRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Stylist> stylists;

}
