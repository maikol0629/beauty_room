package com.mr.sb.beauty_room.Controllers;

import com.mr.sb.beauty_room.DTOS.Auth.RegisterRequest;
import com.mr.sb.beauty_room.DTOS.client.ClientResponseDto;
import com.mr.sb.beauty_room.DTOS.client.ClientSaveDto;
import com.mr.sb.beauty_room.Services.IClientService;
import com.mr.sb.beauty_room.Services.Auth.AuthenticationService;
import com.mr.sb.beauty_room.entities.Client;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Gestión de clientes (incluye telegram_chat_id)")
public class ClientController {
    private final IClientService clientService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Registrar cliente", description = "Registra un nuevo cliente y devuelve JWT. Requiere header X-Tenant-ID")
    public ResponseEntity<?> registerClient(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.registerClient(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientResponseDto> getCurrentClient(@AuthenticationPrincipal Client currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        ClientResponseDto client = clientService.findById(currentUser.getId());
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(client);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientResponseDto> getClientById(@PathVariable Long id, @AuthenticationPrincipal Client currentUser) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        if (!currentUser.getId().equals(id)) {
            return ResponseEntity.status(403).build();
        }
        ClientResponseDto client = clientService.findById(id);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(client);
    }

    @GetMapping
    @PreAuthorize("hasRole('STYLIST')")
    public ResponseEntity<List<ClientResponseDto>> getAllClients() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientResponseDto> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientSaveDto clientDetails,
            @AuthenticationPrincipal Client currentUser) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }
        if (!currentUser.getId().equals(id)) {
            return ResponseEntity.status(403).build();
        }
        boolean updated = clientService.update(clientDetails, id);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        ClientResponseDto updatedClient = clientService.findById(id);
        if (updatedClient == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedClient);
    }
}
