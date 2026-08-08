package com.mr.sb.beauty_room.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI beautyRoomOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Beauty Room API")
                        .description("API REST multitenant para agendamiento de citas en salones de belleza. " +
                                "Tenant por request: claim JWT `tenantId` o header `X-Tenant-ID`.")
                        .version("1.0.0")
                        .contact(new Contact().name("Beauty Room")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
