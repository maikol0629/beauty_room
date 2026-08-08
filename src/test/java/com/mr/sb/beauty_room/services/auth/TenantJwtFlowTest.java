package com.mr.sb.beauty_room.services.auth;

import com.mr.sb.beauty_room.dto.auth.AuthenticationRequest;
import com.mr.sb.beauty_room.dto.auth.AuthenticationResponse;
import com.mr.sb.beauty_room.dto.auth.RegisterRequest;
import com.mr.sb.beauty_room.exceptions.TenantNotResolvedException;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TenantJwtFlowTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void tearDown() {
        TenantInterceptor.clear();
    }

    @Test
    void authenticate_shouldIncludeTenantIdClaim() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john@example.com")
                .password("password")
                .build();

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertThat(jwtService.extractTenantId(response.getToken())).isEqualTo(1L);
    }

    @Test
    void registerClient_shouldAssignTenantFromContextAndEmitClaim() {
        TenantInterceptor.setCurrentTenantId(1L);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = RegisterRequest.builder()
                .email("cliente-" + suffix + "@test.com")
                .password("password")
                .nameClient("Nuevo Cliente")
                .phone("3000000000")
                .build();

        AuthenticationResponse response = authenticationService.registerClient(request);

        assertThat(jwtService.extractTenantId(response.getToken())).isEqualTo(1L);
    }

    @Test
    void registerClient_shouldThrowWhenNoTenantContext() {
        TenantInterceptor.clear();
        RegisterRequest request = RegisterRequest.builder()
                .email("sin-tenant-" + UUID.randomUUID() + "@test.com")
                .password("password")
                .nameClient("Sin Tenant")
                .phone("3000000000")
                .build();

        assertThatThrownBy(() -> authenticationService.registerClient(request))
                .isInstanceOf(TenantNotResolvedException.class);
    }
}
