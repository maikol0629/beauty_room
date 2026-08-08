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
    private String nameRoom;
    @Column(nullable = false)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    private Tenant tenant;

    @OneToMany(mappedBy = "stylistRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Stylist> stylists;

}
