package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.config.SuperAdminInitializer;
import com.mr.sb.beauty_room.dto.superadmin.TenantCreateDto;
import com.mr.sb.beauty_room.dto.superadmin.TenantUpdateDto;
import com.mr.sb.beauty_room.entities.Role;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.TenantPlan;
import com.mr.sb.beauty_room.entities.TenantStatus;
import com.mr.sb.beauty_room.entities.User;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.repository.UserRepository;
import com.mr.sb.beauty_room.services.ISuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Implementación con acceso directo a los repositorios (sin TenantInterceptor):
 * el super admin opera sobre toda la plataforma, no sobre un salón.
 */
@Service
@RequiredArgsConstructor
public class SuperAdminServiceImplement implements ISuperAdminService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final StylistRepository stylistRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<Tenant> findAllTenants() {
        return tenantRepository.findAll().stream()
                .filter(tenant -> !SuperAdminInitializer.PLATFORM_TENANT_KEY.equals(tenant.getTenantKey()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    @Override
    public Tenant findTenantById(Long id) {
        return tenantRepository.findById(id)
                .filter(tenant -> !SuperAdminInitializer.PLATFORM_TENANT_KEY.equals(tenant.getTenantKey()))
                .orElse(null);
    }

    @Transactional
    @Override
    public Tenant createTenant(TenantCreateDto dto) {
        Tenant tenant = Tenant.builder()
                .name(dto.getName())
                .tenantKey(resolveUniqueTenantKey(dto.getTenantKey(), dto.getName()))
                .plan(dto.getPlan() != null ? dto.getPlan() : TenantPlan.TRIAL)
                .status(dto.getStatus() != null ? dto.getStatus() : TenantStatus.ACTIVE)
                .trialEndsAt(dto.getTrialEndsAt())
                .build();
        Tenant saved = tenantRepository.save(tenant);

        // El administrador del salón también es estilista: una fila en users con
        // role ADMIN + su perfil en stylist (mismo id, herencia JOINED).
        userRepository.save(Stylist.builder()
                .email(dto.getAdminEmail())
                .password(passwordEncoder.encode(dto.getAdminPassword()))
                .role(Role.ADMIN)
                .nameStylist(dto.getAdminName())
                .phone(dto.getAdminPhone())
                .tenant(saved)
                .build());
        return saved;
    }

    @Transactional
    @Override
    public Tenant updateTenant(Long id, TenantUpdateDto dto) {
        Tenant tenant = findTenantById(id);
        if (tenant == null) {
            return null;
        }
        tenant.setName(dto.getName());
        tenant.setPlan(dto.getPlan() != null ? dto.getPlan() : tenant.getPlan());
        tenant.setStatus(dto.getStatus() != null ? dto.getStatus() : tenant.getStatus());
        tenant.setTrialEndsAt(dto.getTrialEndsAt());
        return tenantRepository.save(tenant);
    }

    @Transactional
    @Override
    public Tenant setTenantStatus(Long id, TenantStatus status) {
        Tenant tenant = findTenantById(id);
        if (tenant == null) {
            return null;
        }
        tenant.setStatus(status);
        return tenantRepository.save(tenant);
    }

    @Transactional
    @Override
    public boolean deleteTenant(Long id) {
        return setTenantStatus(id, TenantStatus.CANCELLED) != null;
    }

    @Override
    public List<User> findTenantUsers(Long tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    @Override
    public long countTenants() {
        return tenantRepository.count() - 1;
    }

    @Override
    public long countTenantsByStatus(TenantStatus status) {
        return tenantRepository.findAll().stream()
                .filter(t -> t.getStatus() == status
                        && !SuperAdminInitializer.PLATFORM_TENANT_KEY.equals(t.getTenantKey()))
                .count();
    }

    @Override
    public long countTenantsByPlan(TenantPlan plan) {
        return tenantRepository.findAll().stream()
                .filter(t -> t.getPlan() == plan
                        && !SuperAdminInitializer.PLATFORM_TENANT_KEY.equals(t.getTenantKey()))
                .count();
    }

    @Override
    public long countAppointments() {
        return appointmentRepository.count();
    }

    @Override
    public long countClients() {
        return clientRepository.count();
    }

    @Override
    public long countStylists() {
        return stylistRepository.count();
    }

    private String resolveUniqueTenantKey(String candidate, String name) {
        String base = slugify(candidate != null && !candidate.isBlank() ? candidate : name);
        String key = base;
        int suffix = 2;
        while (tenantRepository.findByTenantKey(key).isPresent()) {
            key = base + "-" + suffix++;
        }
        return key;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = normalized.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isBlank() ? "salon" : slug;
    }
}
