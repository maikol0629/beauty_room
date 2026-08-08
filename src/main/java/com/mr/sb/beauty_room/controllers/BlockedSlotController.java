package com.mr.sb.beauty_room.controllers;

import com.mr.sb.beauty_room.dto.blockedslot.BlockedSlotRequestDto;
import com.mr.sb.beauty_room.dto.blockedslot.BlockedSlotResponseDto;
import com.mr.sb.beauty_room.services.IBlockedSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blocked-slot")
@RequiredArgsConstructor
public class BlockedSlotController {

    private final IBlockedSlotService blockedSlotService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<BlockedSlotResponseDto> create(@Valid @RequestBody BlockedSlotRequestDto request) {
        return ResponseEntity.ok(blockedSlotService.create(request));
    }

    @GetMapping("/stylist/{stylistId}")
    public ResponseEntity<List<BlockedSlotResponseDto>> getByStylist(@PathVariable Long stylistId) {
        return ResponseEntity.ok(blockedSlotService.findByStylistId(stylistId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (blockedSlotService.delete(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
