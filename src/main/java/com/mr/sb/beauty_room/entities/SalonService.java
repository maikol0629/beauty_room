package com.mr.sb.beauty_room.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "service")
public class SalonService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String nameService;
    private String description;
    private float price;
    private int duration;

    @ManyToOne
    @JoinColumn(name = "id_stylist")
    @JsonBackReference
    private Stylist stylist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    private Tenant tenant;


}
