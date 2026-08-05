package com.mr.sb.beauty_room.Controllers;

import com.mr.sb.beauty_room.DTOS.stylist_room.StylistRoomRequestDto;
import com.mr.sb.beauty_room.DTOS.stylist_room.StylistRoomResponseDto;
import com.mr.sb.beauty_room.Services.IStylistRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stylist-room")
@RequiredArgsConstructor
public class StylistRoomController {

    private final IStylistRoomService stylistRoomService;

    @GetMapping
    public ResponseEntity<List<StylistRoomResponseDto>> getAll() {
        return ResponseEntity.ok(stylistRoomService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StylistRoomResponseDto> getById(@PathVariable long id) {
        StylistRoomResponseDto room = stylistRoomService.findById(id);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(room);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StylistRoomResponseDto> create(@Valid @RequestBody StylistRoomRequestDto request) {
        return ResponseEntity.ok(stylistRoomService.save(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StylistRoomResponseDto> update(@PathVariable long id, @Valid @RequestBody StylistRoomRequestDto request) {
        StylistRoomResponseDto updated = stylistRoomService.update(id, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (stylistRoomService.deleteById(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
