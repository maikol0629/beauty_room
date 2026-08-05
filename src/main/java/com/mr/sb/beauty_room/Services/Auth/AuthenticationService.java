package com.mr.sb.beauty_room.Services.Auth;

import com.mr.sb.beauty_room.DTOS.Auth.AuthenticationRequest;
import com.mr.sb.beauty_room.DTOS.Auth.AuthenticationResponse;
import com.mr.sb.beauty_room.DTOS.Auth.RegisterRequest;
import com.mr.sb.beauty_room.Exceptions.TenantNotResolvedException;
import com.mr.sb.beauty_room.Exceptions.UserNotFoundException;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.User;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final ClientRepository clientRepository;
    private final StylistRepository stylistRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse registerClient(RegisterRequest request) {
        Tenant tenant = resolveTenantFromContext();
        var client = Client.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name_client(request.getName_client())
                .phone(request.getPhone())
                .tenant(tenant)
                .build();

        clientRepository.save(client);

        var jwtToken = jwtService.generateToken(buildClaims(client), client);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse registerStylist(RegisterRequest request) {
        Tenant tenant = resolveTenantFromContext();
        var stylist = Stylist.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name_stylist(request.getName_stylist())
                .phone(request.getPhone())
                .tenant(tenant)
                .build();

        stylistRepository.save(stylist);

        var jwtToken = jwtService.generateToken(buildClaims(stylist), stylist);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con email: " + request.getEmail()));

        var jwtToken = jwtService.generateToken(buildClaims(user), user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    private Tenant resolveTenantFromContext() {
        Long tenantId = TenantInterceptor.getCurrentTenantId();
        if (tenantId == null) {
            throw new TenantNotResolvedException("No se pudo resolver el tenant para el registro. Envía el header X-Tenant-ID.");
        }
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotResolvedException("El tenant con id " + tenantId + " no existe"));
    }

    private Map<String, Object> buildClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        Long tenantId = (user.getTenant() != null) ? user.getTenant().getId() : null;
        claims.put("tenantId", tenantId);
        return claims;
    }
}
