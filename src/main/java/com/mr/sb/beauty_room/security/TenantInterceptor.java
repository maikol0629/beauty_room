package com.mr.sb.beauty_room.security;

import com.mr.sb.beauty_room.exceptions.TenantNotResolvedException;
import com.mr.sb.beauty_room.exceptions.TenantSuspendedException;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.TenantRepository;
import com.mr.sb.beauty_room.services.auth.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    public static final String X_TENANT_ID_HEADER = "X-Tenant-ID";

    private static final ThreadLocal<Long> tenantIdHolder = new ThreadLocal<>();

    private final JwtService jwtService;
    private final TenantRepository tenantRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long tenantId = resolveFromToken(request);
        if (tenantId == null) {
            tenantId = resolveFromHeader(request);
        }
        if (tenantId != null) {
            assertTenantUsable(tenantId);
            tenantIdHolder.set(tenantId);
        }
        return true;
    }

    private void assertTenantUsable(Long tenantId) {
        Optional<Tenant> tenant = tenantRepository.findById(tenantId);
        if (tenant.isEmpty() || !tenant.get().isUsable()) {
            throw new TenantSuspendedException(
                    "El tenant " + tenantId + " no está disponible: su plan venció o fue suspendido.");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        tenantIdHolder.remove();
    }

    private Long resolveFromToken(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            Claims claims = jwtService.extractAllClaimsForTenant(token);
            return claims.get("tenantId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveFromHeader(HttpServletRequest request) {
        final String tenantHeader = request.getHeader(X_TENANT_ID_HEADER);
        if (tenantHeader == null || tenantHeader.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(tenantHeader.trim());
        } catch (NumberFormatException e) {
            throw new TenantNotResolvedException("Header X-Tenant-ID inválido: " + tenantHeader);
        }
    }

    public static Long getCurrentTenantId() {
        return tenantIdHolder.get();
    }

    public static Long getCurrentTenantIdOrThrow() {
        Long tenantId = tenantIdHolder.get();
        if (tenantId == null) {
            throw new TenantNotResolvedException("No se pudo resolver el tenant para esta solicitud. Envía un JWT válido o el header X-Tenant-ID.");
        }
        return tenantId;
    }

    public static void setCurrentTenantId(Long tenantId) {
        tenantIdHolder.set(tenantId);
    }

    public static void clear() {
        tenantIdHolder.remove();
    }
}
