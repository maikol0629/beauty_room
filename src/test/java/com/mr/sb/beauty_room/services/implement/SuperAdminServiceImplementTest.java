package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.config.SuperAdminInitializer;
import com.mr.sb.beauty_room.dto.superadmin.TenantCreateDto;
import com.mr.sb.beauty_room.dto.superadmin.TenantUpdateDto;
import com.mr.sb.beauty_room.entities.Role;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.TenantPlan;
import com.mr.sb.beauty_room.entities.TenantStatus;
import com.mr.sb.beauty_room.entities.User;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceImplementTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private StylistRepository stylistRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SuperAdminServiceImplement superAdminService;

    @Test
    void createTenant_shouldGenerateSlugKeyAndCreateAdminUser() {
        TenantCreateDto dto = TenantCreateDto.builder()
                .name("Salón Bella")
                .tenantKey("")
                .plan(TenantPlan.BASIC)
                .status(TenantStatus.ACTIVE)
                .adminEmail("admin@salonbella.com")
                .adminPassword("secreto123")
                .build();

        when(tenantRepository.findByTenantKey(anyString())).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(dto.getAdminPassword())).thenReturn("hash-bcrypt");

        Tenant saved = superAdminService.createTenant(dto);

        assertThat(saved.getName()).isEqualTo("Salón Bella");
        assertThat(saved.getTenantKey()).isEqualTo("salon-bella");
        assertThat(saved.getPlan()).isEqualTo(TenantPlan.BASIC);
        assertThat(saved.getStatus()).isEqualTo(TenantStatus.ACTIVE);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertThat(admin.getEmail()).isEqualTo(dto.getAdminEmail());
        assertThat(admin.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getTenant()).isSameAs(saved);
    }

    @Test
    void createTenant_shouldAppendSuffixWhenTenantKeyCollides() {
        TenantCreateDto dto = TenantCreateDto.builder()
                .name("Salón Bella")
                .tenantKey("")
                .plan(TenantPlan.TRIAL)
                .status(TenantStatus.ACTIVE)
                .adminEmail("admin@salonbella.com")
                .adminPassword("password")
                .build();

        Tenant existing = Tenant.builder().id(1L).tenantKey("salon-bella").build();
        when(tenantRepository.findByTenantKey("salon-bella")).thenReturn(Optional.of(existing));
        when(tenantRepository.findByTenantKey("salon-bella-2")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        Tenant saved = superAdminService.createTenant(dto);

        assertThat(saved.getTenantKey()).isEqualTo("salon-bella-2");
    }

    @Test
    void createTenant_shouldHonorProvidedTenantKey() {
        TenantCreateDto dto = TenantCreateDto.builder()
                .name("Salón Bella")
                .tenantKey("mi-key-personalizada")
                .plan(TenantPlan.TRIAL)
                .status(TenantStatus.ACTIVE)
                .adminEmail("admin@salonbella.com")
                .adminPassword("password")
                .build();

        when(tenantRepository.findByTenantKey(anyString())).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        Tenant saved = superAdminService.createTenant(dto);

        assertThat(saved.getTenantKey()).isEqualTo("mi-key-personalizada");
    }

    @Test
    void findAllTenants_shouldExcludePlatformTenantAndSortNewestFirst() {
        Tenant platform = Tenant.builder().id(99L)
                .tenantKey(SuperAdminInitializer.PLATFORM_TENANT_KEY)
                .name("Plataforma")
                .createdAt(LocalDateTime.now().minusDays(10)).build();
        Tenant older = Tenant.builder().id(1L).tenantKey("salon-a")
                .createdAt(LocalDateTime.now().minusDays(5)).build();
        Tenant newer = Tenant.builder().id(2L).tenantKey("salon-b")
                .createdAt(LocalDateTime.now().minusDays(1)).build();

        when(tenantRepository.findAll()).thenReturn(List.of(platform, newer, older));

        List<Tenant> result = superAdminService.findAllTenants();

        assertThat(result).extracting(Tenant::getTenantKey)
                .containsExactly("salon-b", "salon-a");
    }

    @Test
    void findTenantById_shouldReturnNullForPlatformTenant() {
        Tenant platform = Tenant.builder().id(99L)
                .tenantKey(SuperAdminInitializer.PLATFORM_TENANT_KEY).build();
        when(tenantRepository.findById(99L)).thenReturn(Optional.of(platform));

        assertThat(superAdminService.findTenantById(99L)).isNull();
    }

    @Test
    void updateTenant_shouldApplyChanges() {
        Tenant existing = Tenant.builder().id(1L).tenantKey("salon-x").name("Antes")
                .plan(TenantPlan.TRIAL).status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantUpdateDto dto = TenantUpdateDto.builder()
                .name("Después")
                .plan(TenantPlan.PREMIUM)
                .status(TenantStatus.SUSPENDED)
                .trialEndsAt(LocalDateTime.of(2026, 12, 31, 23, 59))
                .build();

        Tenant updated = superAdminService.updateTenant(1L, dto);

        assertThat(updated.getName()).isEqualTo("Después");
        assertThat(updated.getPlan()).isEqualTo(TenantPlan.PREMIUM);
        assertThat(updated.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(updated.getTrialEndsAt()).isEqualTo(dto.getTrialEndsAt());
    }

    @Test
    void updateTenant_shouldReturnNullWhenNotFound() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());

        TenantUpdateDto dto = TenantUpdateDto.builder()
                .name("X").plan(TenantPlan.TRIAL).status(TenantStatus.ACTIVE).build();

        assertThat(superAdminService.updateTenant(1L, dto)).isNull();
    }

    @Test
    void setTenantStatus_shouldUpdateStatus() {
        Tenant existing = Tenant.builder().id(1L).tenantKey("salon-x")
                .status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant updated = superAdminService.setTenantStatus(1L, TenantStatus.SUSPENDED);

        assertThat(updated.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }
}
