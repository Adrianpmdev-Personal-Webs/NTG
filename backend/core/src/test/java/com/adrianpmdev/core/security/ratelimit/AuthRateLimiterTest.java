package com.adrianpmdev.core.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Limits on the session endpoints: per email, per client address and the client address itself.
 *
 * @author adrianpmdev
 */
class AuthRateLimiterTest {

    private final AuthRateLimiter limiter = new AuthRateLimiter();

    @Test
    void tooManySignInsForOneEmailAreRefused() {
        for (int attempt = 0; attempt < AuthRateLimiter.LOGIN_PER_EMAIL; attempt++) {
            limiter.checkLogin(requestFrom("203.0.113.1"), "Player@Example.com");
        }

        // Same email written differently, from another address: still the same account.
        assertThatThrownBy(() -> limiter.checkLogin(requestFrom("203.0.113.2"), " player@example.com "))
                .isInstanceOfSatisfying(TooManyRequestsException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(Long.parseLong(exception.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))).isPositive();
                });
    }

    @Test
    void tooManySignInsFromOneAddressAreRefused() {
        for (int attempt = 0; attempt < AuthRateLimiter.LOGIN_PER_ADDRESS; attempt++) {
            limiter.checkLogin(requestFrom("198.51.100.7"), "user" + attempt + "@example.com");
        }

        assertThatThrownBy(() -> limiter.checkLogin(requestFrom("198.51.100.7"), "another@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
        limiter.checkLogin(requestFrom("198.51.100.8"), "another@example.com");
    }

    @Test
    void passwordFromEmailLinkIsLimited() {
        for (int attempt = 0; attempt < AuthRateLimiter.PASSWORD_PER_ADDRESS; attempt++) {
            limiter.checkSetPassword(requestFrom("192.0.2.10"));
        }

        assertThatThrownBy(() -> limiter.checkSetPassword(requestFrom("192.0.2.10")))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void clientAddressIsTheFirstForwardedOne() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 76.76.21.21");

        assertThat(AuthRateLimiter.clientAddress(request)).isEqualTo("203.0.113.9");
        assertThat(AuthRateLimiter.clientAddress(new MockHttpServletRequest())).isEqualTo("127.0.0.1");
    }

    private static MockHttpServletRequest requestFrom(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", address);

        return request;
    }
}
