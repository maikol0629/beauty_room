package com.mr.sb.beauty_room.controllers;

import com.mr.sb.beauty_room.dto.auth.AuthenticationResponse;
import com.mr.sb.beauty_room.dto.auth.RegisterRequest;
import com.mr.sb.beauty_room.services.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro, login y emisión de JWT (con claim tenantId)")
public class
AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register/client")
    @Operation(summary = "Registrar cliente",
            description = "Registra un cliente. Requiere header X-Tenant-ID si no se envía JWT. Devuelve JWT con claim tenantId")
    public ResponseEntity<AuthenticationResponse> registerClient(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.registerClient(request));
    }

    @PostMapping("/register/stylist")
    @Operation(summary = "Registrar estilista",
            description = "Registra un estilista. Requiere header X-Tenant-ID si no se envía JWT. Devuelve JWT con claim tenantId")
    public ResponseEntity<AuthenticationResponse> registerStylist(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.registerStylist(request));
    }

    @PostMapping("/authenticate")
    @Operation(summary = "Autenticar usuario",
            description = "Login con email+password. Devuelve JWT con claims userId, role y tenantId")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody com.mr.sb.beauty_room.dto.auth.AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
}