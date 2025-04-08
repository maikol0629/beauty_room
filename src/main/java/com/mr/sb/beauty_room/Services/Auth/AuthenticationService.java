package com.mr.sb.beauty_room.Services.Auth;

import com.mr.sb.beauty_room.DTOS.Auth.AuthenticationRequest;
import com.mr.sb.beauty_room.DTOS.Auth.AuthenticationResponse;
import com.mr.sb.beauty_room.DTOS.Auth.RegisterRequest;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final ClientRepository clientRepository;
    private final StylistRepository stylistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse registerClient(RegisterRequest request) {
        var client = Client.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name_client(request.getName_client())
                .phone(request.getPhone())
                .build();
        
        clientRepository.save(client);
        
        var jwtToken = jwtService.generateToken(client);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse registerStylist(RegisterRequest request) {
        var stylist = Stylist.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name_stylist(request.getName_stylist())
                .phone(request.getPhone())
                .build();
        
        stylistRepository.save(stylist);
        
        var jwtToken = jwtService.generateToken(stylist);
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
        
        var client = clientRepository.findByEmail(request.getEmail());
        if (client.isPresent()) {
            var jwtToken = jwtService.generateToken(client.get());
            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .build();
        }
        
        var stylist = stylistRepository.findByEmail(request.getEmail());
        if (stylist.isPresent()) {
            var jwtToken = jwtService.generateToken(stylist.get());
            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .build();
        }
        
        throw new RuntimeException("Usuario no encontrado");
    }
} 