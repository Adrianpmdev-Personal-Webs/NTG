package com.adrianpmdev.core.security.config;

import com.adrianpmdev.core.security.authentication.SupabaseBasicAdminAuthenticationProvider;
import com.adrianpmdev.core.security.authorization.Authorities;
import com.adrianpmdev.core.security.filter.SupabaseJwtAuthFilter;
import com.adrianpmdev.core.config.AppProperties;
import com.adrianpmdev.core.features.users.infrastructure.persistence.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

/**
 * Security configuration.
 * Swagger is protected with Basic Auth (ADMIN) and the API with Supabase access tokens.
 * The API is deny-by-default: every route must be listed explicitly.
 *
 * @author adrianpmdev
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ROLE_ADMIN = Authorities.ROLE_PREFIX + ADMIN_ROLE;
    private static final String UNAUTHENTICATED_MESSAGE = "Unauthenticated";
    private static final String ACCESS_DENIED_MESSAGE = "Access denied";

    private static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };

    /**
     * Game modules. Reading under the path requires the tag (or ADMIN); writing requires ADMIN.
     * To add a game, add its base path and tag key here, create the tag, and assign it to users.
     */
    private static final Map<String, String> GAME_TAGS = Map.of(
            "/api/v1/pokemon/**", "pokemon",
            "/api/v1/wow-forever/**", "wow-forever"
    );

    /** Team picture of a player inside a Pokémon tournament (upload and remove). */
    private static final String POKEMON_TEAM_IMAGE = "/api/v1/pokemon/tournament-players/*/team-image";

    private static final long CORS_MAX_AGE_SECONDS = 3_600;

    /**
     * Security chain for Swagger endpoints (Basic Auth, ADMIN only).
     *
     * @param http          HTTP security configuration
     * @param basicProvider Basic Auth admin authentication provider
     * @return configured security filter chain
     * @throws Exception if the security chain cannot be built
     */
    @Bean
    @Order(1)
    SecurityFilterChain swaggerChain(
            HttpSecurity http,
            SupabaseBasicAdminAuthenticationProvider basicProvider) throws Exception {
        http
                .securityMatcher(SWAGGER_ENDPOINTS)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authenticationProvider(basicProvider)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(ADMIN_ROLE))
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    /**
     * Security chain for API endpoints.
     *
     * @param http           HTTP security configuration
     * @param jwtDecoder     access token decoder
     * @param userRepository local user repository
     * @return configured security filter chain
     * @throws Exception if the security chain cannot be built
     */
    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http, JwtDecoder jwtDecoder, UserRepository userRepository) throws Exception {
        SupabaseJwtAuthFilter jwtFilter = new SupabaseJwtAuthFilter(jwtDecoder, userRepository);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers("/error", "/keep-alive").permitAll();

                    // Session: public entry points, the rest of the auth routes need a session.
                    auth.requestMatchers(HttpMethod.POST,
                            "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll();
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/auth/set-password").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated();

                    // Users: anyone logged in can edit their own profile; everything else is ADMIN.
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/users/me").authenticated();
                    auth.requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole(ADMIN_ROLE);
                    auth.requestMatchers("/api/v1/tags", "/api/v1/tags/**").hasRole(ADMIN_ROLE);

                    // Pokémon team pictures: the only game writes open to players. The pokemon module then
                    // accepts only the player of that tournament entry (or an admin).
                    auth.requestMatchers(HttpMethod.PUT, POKEMON_TEAM_IMAGE).hasAnyAuthority(ROLE_ADMIN, Authorities.tag("pokemon"));
                    auth.requestMatchers(HttpMethod.DELETE, POKEMON_TEAM_IMAGE).hasAnyAuthority(ROLE_ADMIN, Authorities.tag("pokemon"));

                    // Games: read with the game tag, write as ADMIN.
                    GAME_TAGS.forEach((pattern, tag) -> {
                        auth.requestMatchers(HttpMethod.GET, pattern).hasAnyAuthority(ROLE_ADMIN, Authorities.tag(tag));
                        auth.requestMatchers(pattern).hasRole(ADMIN_ROLE);
                    });

                    auth.anyRequest().denyAll();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) -> response.sendError(401, UNAUTHENTICATED_MESSAGE))
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> response.sendError(403, ACCESS_DENIED_MESSAGE)));

        return http.build();
    }

    /**
     * CORS configuration used by Spring Security (origins come from {@code app.cors.allowed-origins}).
     *
     * @param appProperties application settings
     * @return CORS configuration source
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appProperties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(CORS_MAX_AGE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}