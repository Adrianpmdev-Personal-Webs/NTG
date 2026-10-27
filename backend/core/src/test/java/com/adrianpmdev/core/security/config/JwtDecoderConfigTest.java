package com.adrianpmdev.core.security.config;

import com.adrianpmdev.core.integration.supabase.config.SupabaseProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Local verification of Supabase access tokens: signature, expiry, issuer and audience.
 *
 * @author adrianpmdev
 */
class JwtDecoderConfigTest {

    private static final String URL = "https://example.supabase.co";
    private static final String ISSUER = URL + "/auth/v1";
    private static final String SECRET = "a-test-secret-that-is-long-enough-for-hs256!!";

    private final JwtDecoder decoder = new JwtDecoderConfig().supabaseJwtDecoder(new SupabaseProperties(URL, "anon",
            "service", new SupabaseProperties.Jwt(null, "ES256", SECRET, null, "authenticated")));

    @Test
    void sessionTokenIsAccepted() throws Exception {
        assertThat(decoder.decode(token(ISSUER, "authenticated")).getSubject()).isEqualTo("user-1");
    }

    @Test
    void tokenForAnotherAudienceIsRefused() {
        assertThatThrownBy(() -> decoder.decode(token(ISSUER, "anon"))).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRefused() {
        assertThatThrownBy(() -> decoder.decode(token("https://evil.example/auth/v1", "authenticated")))
                .isInstanceOf(JwtValidationException.class);
    }

    private static String token(String issuer, String audience) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user-1")
                .issuer(issuer)
                .audience(audience)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET.getBytes()));

        return jwt.serialize();
    }
}
