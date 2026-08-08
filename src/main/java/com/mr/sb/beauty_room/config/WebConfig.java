package com.mr.sb.beauty_room.config;

import com.mr.sb.beauty_room.security.TenantInterceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    @NonNull
    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**");
    }
}
