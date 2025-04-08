package com.mr.sb.beauty_room.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class StylistSchedule {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne
        @JsonBackReference
        @JoinColumn(name = "stylist_id", nullable = false)
        private Stylist stylist;

        @Enumerated(EnumType.STRING)
        private DayOfWeek day;

        private LocalTime startTime;
        private LocalTime endTime;

}
