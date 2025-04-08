package com.mr.sb.beauty_room.Security;

import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final ClientRepository clientRepository;
    private final StylistRepository stylistRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Primero intenta encontrar un cliente
        return clientRepository.findByEmail(email)
                .map(client -> (UserDetails) client)
                .orElseGet(() -> {
                    // Si no es cliente, intenta encontrar un estilista
                    return stylistRepository.findByEmail(email)
                            .map(stylist -> (UserDetails) stylist)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
                });
    }
} 