package com.mr.sb.beauty_room.Controllers;

import com.mr.sb.beauty_room.DTOS.Auth.AuthenticationResponse;
import com.mr.sb.beauty_room.DTOS.Auth.RegisterRequest;
import com.mr.sb.beauty_room.Services.Auth.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class
AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register/client")
    public ResponseEntity<AuthenticationResponse> registerClient(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.registerClient(request));
    }

    @PostMapping("/register/stylist")
    public ResponseEntity<AuthenticationResponse> registerStylist(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.registerStylist(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody com.mr.sb.beauty_room.DTOS.Auth.AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
} 