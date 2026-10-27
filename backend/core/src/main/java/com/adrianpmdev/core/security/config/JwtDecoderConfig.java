package com.adrianpmdev.core.security.config;

import com.adrianpmdev.core.integration.supabase.config.SupabaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Verifies Supabase access tokens locally (signature, expiry, issuer and audience) without calling Supabase.
 * Uses the project JWKS endpoint (asymmetric signing keys) or, if {@code supabase.jwt.secret}
 * is set, the legacy HS256 shared secret.
 *
 * @author adrianpmdev
 */
@Configuration
public class JwtDecoderConfig {

    /**
     * Builds the access token decoder.
     *
     * @param properties Supabase settings
     * @return JWT decoder
     */
    @Bean
    public JwtDecoder supabaseJwtDecoder(SupabaseProperties properties) {
        NimbusJwtDecoder decoder;
        String secret = properties.jwt().secret();

        if (secret != null && !secret.isBlank()) {
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        } else {
            SignatureAlgorithm algorithm = SignatureAlgorithm.from(properties.jwt().algorithm());

            if (algorithm == null) {
                throw new IllegalStateException("Unsupported supabase.jwt.algorithm: " + properties.jwt().algorithm());
            }

            decoder = NimbusJwtDecoder.withJwkSetUri(properties.resolvedJwksUri()).jwsAlgorithm(algorithm).build();
        }

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(properties.resolvedIssuer());
        String audience = properties.jwt().audience();

        // Only session tokens: other tokens signed by the same project (another audience) are refused.
        if (audience != null && !audience.isBlank()) {
            validator = new DelegatingOAuth2TokenValidator<>(validator, new JwtClaimValidator<List<String>>(
                    JwtClaimNames.AUD, audiences -> audiences != null && audiences.contains(audience)));
        }

        decoder.setJwtValidator(validator);

        return decoder;
    }
}