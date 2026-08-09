package com.mr.sb.beauty_room.config;

import com.mr.sb.beauty_room.entities.Role;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.TenantPlan;
import com.mr.sb.beauty_room.entities.TenantStatus;
import com.mr.sb.beauty_room.entities.User;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea (si no existen) el tenant de plataforma y el usuario SUPER_ADMIN.
 * El super admin no pertenece a ningún salón: vive en un tenant reservado
 * (beauty-room-platform) que se oculta de los listados de salones.
 */
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements ApplicationRunner {

    public static final String PLATFORM_TENANT_KEY = "beauty-room-platform";
    public static final String PLATFORM_TENANT_NAME = "Beauty Room Plataforma";

    private final SuperAdminProperties properties;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Tenant platformTenant = tenantRepository.findByTenantKey(PLATFORM_TENANT_KEY)
                .orElseGet(() -> tenantRepository.save(
                        Tenant.builder()
                                .name(PLATFORM_TENANT_NAME)
                                .tenantKey(PLATFORM_TENANT_KEY)
                                .plan(TenantPlan.PREMIUM)
                                .status(TenantStatus.ACTIVE)
                                .build()));

        if (userRepository.findByEmail(properties.getEmail()).isEmpty()) {
            userRepository.save(User.builder()
                    .email(properties.getEmail())
                    .password(passwordEncoder.encode(properties.getPassword()))
                    .role(Role.SUPER_ADMIN)
                    .tenant(platformTenant)
                    .build());
        }
    }
}
