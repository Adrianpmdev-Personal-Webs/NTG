package com.adrianpmdev.core.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Assigns a request id (header {@code X-Request-Id}, put in the log context as {@code requestId})
 * and writes one readable access line per request. 
 *
 * @author adrianpmdev
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_KEY = "requestId";

    private static final Logger LOG = LoggerFactory.getLogger("ACCESS");
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final String KEEP_ALIVE_PATH = "/keep-alive";

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        String requestId = incoming != null && VALID_REQUEST_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString().substring(0, 12);

        MDC.put(REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long start = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (!KEEP_ALIVE_PATH.equals(request.getRequestURI())) {
                LOG.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), (System.nanoTime() - start) / 1_000_000);
            }

            MDC.remove(REQUEST_ID_KEY);
        }
    }
}