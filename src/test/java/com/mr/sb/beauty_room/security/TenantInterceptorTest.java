package com.mr.sb.beauty_room.security;

import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.TenantStatus;
import com.mr.sb.beauty_room.exceptions.TenantSuspendedException;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.services.auth.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final TenantInterceptor interceptor = new TenantInterceptor(jwtService, tenantRepository);

    @AfterEach
    void tearDown() {
        TenantInterceptor.clear();
    }

    @Test
    void preHandle_activeTenant_shouldSetTenantAndReturnTrue() {
        Tenant active = Tenant.builder().id(7L).status(TenantStatus.ACTIVE)
                .trialEndsAt(LocalDateTime.now().plusDays(5)).build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(active));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantInterceptor.X_TENANT_ID_HEADER, "7");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), null);

        assertThat(result).isTrue();
        assertThat(TenantInterceptor.getCurrentTenantId()).isEqualTo(7L);
    }

    @Test
    void preHandle_activeTenantWithoutExpiry_shouldAllowAccess() {
        Tenant active = Tenant.builder().id(7L).status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(active));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantInterceptor.X_TENANT_ID_HEADER, "7");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void preHandle_suspendedTenant_shouldThrowTenantSuspended() {
        Tenant suspended = Tenant.builder().id(7L).status(TenantStatus.SUSPENDED).build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(suspended));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantInterceptor.X_TENANT_ID_HEADER, "7");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), null))
                .isInstanceOf(TenantSuspendedException.class);
    }

    @Test
    void preHandle_expiredTrialTenant_shouldThrowTenantSuspended() {
        Tenant expired = Tenant.builder().id(7L).status(TenantStatus.ACTIVE)
                .trialEndsAt(LocalDateTime.now().minusDays(1)).build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(expired));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantInterceptor.X_TENANT_ID_HEADER, "7");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), null))
                .isInstanceOf(TenantSuspendedException.class);
    }

    @Test
    void preHandle_unknownTenant_shouldThrowTenantSuspended() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantInterceptor.X_TENANT_ID_HEADER, "99");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), null))
                .isInstanceOf(TenantSuspendedException.class);
    }

    @Test
    void preHandle_withoutTenantHeader_shouldNotSetContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), null);

        assertThat(result).isTrue();
        assertThat(TenantInterceptor.getCurrentTenantId()).isNull();
    }

    @Test
    void afterCompletion_shouldClearTenantContext() {
        Tenant active = Tenant.builder().id(7L).status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(active));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantInterceptor.X_TENANT_ID_HEADER, "7");
        interceptor.preHandle(request, new MockHttpServletResponse(), null);

        interceptor.afterCompletion(request, new MockHttpServletResponse(), null, null);

        assertThat(TenantInterceptor.getCurrentTenantId()).isNull();
    }
}
