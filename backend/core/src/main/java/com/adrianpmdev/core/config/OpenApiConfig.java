package com.adrianpmdev.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for Swagger JWT authentication.
 *
 * @author adrianpmdev
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String BEARER_SCHEME = "bearer";
    private static final String JWT_FORMAT = "JWT";

    /**
     * Configures Swagger to use JWT Bearer authentication.
     *
     * @return OpenAPI configuration with Bearer authentication
     */
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, buildBearerSecurityScheme()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    /**
     * Builds the JWT Bearer security scheme.
     *
     * @return JWT Bearer security scheme
     */
    private SecurityScheme buildBearerSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme(BEARER_SCHEME)
                .bearerFormat(JWT_FORMAT);
    }
}