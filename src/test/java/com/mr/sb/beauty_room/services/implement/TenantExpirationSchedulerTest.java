package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.config.SuperAdminInitializer;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.TenantStatus;
import com.mr.sb.beauty_room.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantExpirationSchedulerTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantExpirationScheduler scheduler;

    @Test
    void suspendExpiredTrials_shouldSuspendActiveTenantsWhoseTrialEnded() {
        Tenant expired = Tenant.builder()
                .id(1L).tenantKey("salon-a").name("Salón A")
                .status(TenantStatus.ACTIVE)
                .trialEndsAt(LocalDateTime.now().minusDays(1))
                .build();
        when(tenantRepository.findByStatusAndTrialEndsAtBefore(eq(TenantStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        scheduler.suspendExpiredTrials();

        assertThat(expired.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        verify(tenantRepository).save(expired);
    }

    @Test
    void suspendExpiredTrials_shouldNotTouchPlatformTenant() {
        Tenant platform = Tenant.builder()
                .id(99L).tenantKey(SuperAdminInitializer.PLATFORM_TENANT_KEY).name("Plataforma")
                .status(TenantStatus.ACTIVE)
                .trialEndsAt(LocalDateTime.now().minusDays(1))
                .build();
        Tenant expired = Tenant.builder()
                .id(2L).tenantKey("salon-b").name("Salón B")
                .status(TenantStatus.ACTIVE)
                .trialEndsAt(LocalDateTime.now().minusDays(1))
                .build();
        when(tenantRepository.findByStatusAndTrialEndsAtBefore(eq(TenantStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(platform, expired));

        scheduler.suspendExpiredTrials();

        assertThat(platform.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(expired.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        verify(tenantRepository, times(1)).save(expired);
        verify(tenantRepository, never()).save(platform);
    }

    @Test
    void suspendExpiredTrials_noExpiredTenants_shouldDoNothing() {
        when(tenantRepository.findByStatusAndTrialEndsAtBefore(eq(TenantStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.suspendExpiredTrials();

        verify(tenantRepository, never()).save(org.mockito.ArgumentMatchers.any(Tenant.class));
    }
}
