package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.config.SuperAdminInitializer;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.TenantStatus;
import com.mr.sb.beauty_room.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Suspende automáticamente los tenants cuyo trial venció: convierte la fecha de
 * caducidad en estado (TenantStatus), que es la fuente de verdad para los
 * puntos de enforcement (API, panel y bot). El tenant de plataforma nunca se suspende.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantExpirationScheduler {

    private final TenantRepository tenantRepository;

    @Scheduled(cron = "${app.tenants.expiration-check:0 0 * * * *}")
    public void suspendExpiredTrials() {
        LocalDateTime now = LocalDateTime.now();
        List<Tenant> expired = tenantRepository.findByStatusAndTrialEndsAtBefore(TenantStatus.ACTIVE, now);
        for (Tenant tenant : expired) {
            if (SuperAdminInitializer.PLATFORM_TENANT_KEY.equals(tenant.getTenantKey())) {
                continue;
            }
            tenant.setStatus(TenantStatus.SUSPENDED);
            tenantRepository.save(tenant);
            log.info("Tenant '{}' (id={}) suspendido automáticamente por fin del trial en {}", tenant.getName(), tenant.getId(), tenant.getTrialEndsAt());
        }
    }
}
