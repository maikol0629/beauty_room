package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.stylist.StylistSaveDto;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.entities.Role;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.repository.StylistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StylistServiceImplementTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private StylistRepository stylistRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StylistServiceImplement stylistService;

    @BeforeEach
    void setUp() {
        TenantInterceptor.setCurrentTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantInterceptor.clear();
    }

    @Test
    void save_shouldPersistStylistWithEncodedPasswordAndTenant() {
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encoded");

        StylistSaveDto dto = StylistSaveDto.builder()
                .name("Ana Estilista")
                .email("ana@example.com")
                .password("secret123")
                .phone("5551234")
                .build();

        boolean ok = stylistService.save(dto);

        assertThat(ok).isTrue();
        ArgumentCaptor<Stylist> captor = ArgumentCaptor.forClass(Stylist.class);
        verify(stylistRepository).save(captor.capture());
        Stylist saved = captor.getValue();
        assertThat(saved.getNameStylist()).isEqualTo(dto.getName());
        assertThat(saved.getEmail()).isEqualTo(dto.getEmail());
        assertThat(saved.getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(saved.getPhone()).isEqualTo(dto.getPhone());
        assertThat(saved.getTenant().getId()).isEqualTo(TENANT_ID);
    }

    @Test
    void save_shouldReturnFalseWhenPasswordBlank() {
        StylistSaveDto dto = StylistSaveDto.builder()
                .name("Ana Estilista")
                .email("ana@example.com")
                .password("   ")
                .phone("5551234")
                .build();

        boolean ok = stylistService.save(dto);

        assertThat(ok).isFalse();
        verify(stylistRepository, never()).save(any());
    }

    @Test
    void save_shouldReturnFalseWhenNameBlank() {
        StylistSaveDto dto = StylistSaveDto.builder()
                .name("   ")
                .email("ana@example.com")
                .password("secret123")
                .phone("5551234")
                .build();

        boolean ok = stylistService.save(dto);

        assertThat(ok).isFalse();
        verify(stylistRepository, never()).save(any());
    }

    @Test
    void save_shouldGenerateVincularCode() {
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encoded");
        StylistSaveDto dto = StylistSaveDto.builder()
                .name("Ana Estilista")
                .email("ana@example.com")
                .password("secret123")
                .phone("5551234")
                .build();

        stylistService.save(dto);

        ArgumentCaptor<Stylist> captor = ArgumentCaptor.forClass(Stylist.class);
        verify(stylistRepository).save(captor.capture());
        assertThat(captor.getValue().getVincularCode()).isNotBlank().hasSize(16);
    }

    @Test
    void prePersist_shouldKeepExplicitRole() {
        Stylist stylist = Stylist.builder().role(Role.ADMIN).build();
        stylist.prePersist();
        assertThat(stylist.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void prePersist_shouldDefaultRoleToStylistWhenNull() {
        Stylist stylist = Stylist.builder().build();
        stylist.prePersist();
        assertThat(stylist.getRole()).isEqualTo(Role.STYLIST);
    }

    @Test
    void regenerateVincularCode_shouldGenerateNewCodeForExistingStylist() {
        Stylist stylist = new Stylist();
        stylist.setId(1L);
        when(stylistRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(stylist));
        when(stylistRepository.save(any(Stylist.class))).thenAnswer(inv -> inv.getArgument(0));

        String code = stylistService.regenerateVincularCode(1L);

        assertThat(code).isNotBlank().hasSize(16);
        assertThat(stylist.getVincularCode()).isEqualTo(code);
    }

    @Test
    void regenerateVincularCode_stylistNotFound_shouldReturnNull() {
        when(stylistRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThat(stylistService.regenerateVincularCode(99L)).isNull();
    }
}
