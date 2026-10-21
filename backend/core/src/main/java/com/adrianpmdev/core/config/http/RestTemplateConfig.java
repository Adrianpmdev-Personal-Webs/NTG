package com.adrianpmdev.core.config.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the shared RestTemplate bean.
 * Centralizes HTTP timeouts and allows easier mocking in tests.
 *
 * @author adrianpmdev
 */
@Configuration
public class RestTemplateConfig {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    /**
     * Builds the shared RestTemplate instance.
     *
     * @return RestTemplate configured with connection and read timeouts
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);

        return new RestTemplate(requestFactory);
    }
}