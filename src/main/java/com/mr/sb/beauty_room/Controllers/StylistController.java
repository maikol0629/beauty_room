package com.mr.sb.beauty_room.Controllers;

import com.mr.sb.beauty_room.DTOS.Auth.RegisterRequest;
import com.mr.sb.beauty_room.DTOS.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist.StylistSaveDto;
import com.mr.sb.beauty_room.Services.Auth.AuthenticationService;
import com.mr.sb.beauty_room.Services.IStylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stylist")
@RequiredArgsConstructor
@Tag(name = "Stylists", description = "Gestión de estilistas (endpoint público con header X-Tenant-ID)")
public class StylistController {
    private final IStylistService stylistService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar estilista", description = "Registra un nuevo estilista (solo ADMIN)")
    public ResponseEntity<?> registerStylist(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.registerStylist(request));
    }

    @GetMapping("/public")
    @Operation(summary = "Listar estilistas (público)",
            description = "Lista los estilistas del tenant. Requiere header X-Tenant-ID (sin JWT)")
    public ResponseEntity<List<StylistResponseDto>> getAllStylistsPublic() {
        return ResponseEntity.ok(stylistService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<StylistResponseDto> getStylistById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        StylistResponseDto stylist = stylistService.findById(id);
        if (stylist == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stylist);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<List<StylistResponseDto>> getAllStylists() {
        return ResponseEntity.ok(stylistService.findAll());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<StylistResponseDto> updateStylist(
            @PathVariable Long id,
            @Valid @RequestBody StylistSaveDto stylistDetails) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        boolean updated = stylistService.update(stylistDetails, id);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        StylistResponseDto updatedStylist = stylistService.findById(id);
        if (updatedStylist == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedStylist);
    }
}
