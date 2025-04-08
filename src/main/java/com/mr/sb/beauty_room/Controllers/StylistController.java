package com.mr.sb.beauty_room.Controllers;

import com.mr.sb.beauty_room.DTOS.Auth.RegisterRequest;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.Services.Auth.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stylist")
@RequiredArgsConstructor
public class StylistController {

    private final StylistRepository stylistRepository;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerStylist(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.registerStylist(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<Stylist> getStylistById(@PathVariable Long id) {
        return stylistRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<List<Stylist>> getAllStylists() {
        return ResponseEntity.ok(stylistRepository.findAll());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<Stylist> updateStylist(
            @PathVariable Long id,
            @Valid @RequestBody Stylist stylistDetails) {
        return stylistRepository.findById(id)
                .map(existingStylist -> {
                    existingStylist.setName_stylist(stylistDetails.getName_stylist());
                    existingStylist.setPhone(stylistDetails.getPhone());
                    return ResponseEntity.ok(stylistRepository.save(existingStylist));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
