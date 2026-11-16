package com.example.resumeanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds AI provider configuration read from application.properties
 * (which in turn reads the AI_API_KEY environment variable).
 * <p>
 * Deliberately does NOT fail application startup when the key is
 * missing - {@link #isConfigured()} lets the service layer decide
 * what to do (use demo fallback, or return a clear error) only when
 * an actual analysis is requested.
 */
@Component
public class AIConfig {

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.provider}")
    private String provider;

    @Value("${ai.api.model}")
    private String model;

    @Value("${ai.api.timeout-seconds}")
    private int timeoutSeconds;

    @Value("${ai.demo-fallback-enabled}")
    private boolean demoFallbackEnabled;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean isDemoFallbackEnabled() {
        return demoFallbackEnabled;
    }
}
