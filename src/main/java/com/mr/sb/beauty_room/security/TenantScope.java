package com.mr.sb.beauty_room.security;

import java.util.function.Supplier;

public final class TenantScope {

    private TenantScope() {
    }

    public static <T> T withTenant(Long tenantId, Supplier<T> supplier) {
        TenantInterceptor.setCurrentTenantId(tenantId);
        try {
            return supplier.get();
        } finally {
            TenantInterceptor.clear();
        }
    }

    public static void runWithTenant(Long tenantId, Runnable runnable) {
        withTenant(tenantId, () -> {
            runnable.run();
            return null;
        });
    }
}
