package com.mr.sb.beauty_room.Controllers.panel;

import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.User;
import com.mr.sb.beauty_room.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PanelTenantHelper {

    private final UserRepository userRepository;

    public Long currentTenantId() {
        Tenant tenant = currentTenant();
        return tenant.getId();
    }

    public Tenant currentTenant() {
        User user = currentUser();
        Tenant tenant = user.getTenant();
        if (tenant == null) {
            throw new IllegalStateException("No se pudo resolver el tenant del usuario logueado en el panel");
        }
        return tenant;
    }

    private User currentUser() {
        Long userId = currentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario logueado en el panel"));
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("No hay usuario autenticado en el panel");
    }

    public <T> T withTenant(Supplier<T> action) {
        Long tenantId = currentTenantId();
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            return action.get();
        } finally {
            TenantInterceptor.clear();
        }
    }
}
