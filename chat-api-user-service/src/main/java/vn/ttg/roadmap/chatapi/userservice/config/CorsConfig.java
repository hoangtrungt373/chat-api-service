package vn.ttg.roadmap.chatapi.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for browser-based clients (React/Vite frontend).
 *
 * Needed for the OAuth2 "state token" flow where the frontend calls:
 * POST /api/v1/auth/exchange-state from a different origin (e.g. http://localhost:3000).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend.url:http://localhost:3000}") String frontendUrl
    ) {
        CorsConfiguration config = new CorsConfiguration();

        // For local dev you typically want exactly one allowed origin (your frontend).
        // NOTE: Must be the exact origin (scheme + host + port), not a path.
        config.setAllowedOrigins(Arrays.asList(frontendUrl));

        // Allow common methods + preflight
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow typical headers used by fetch()
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin"));

        // If you ever switch to cookies, you must also set allowCredentials(true)
        // and use setAllowedOriginPatterns (not "*").
        config.setAllowCredentials(false);

        // Cache preflight response (seconds)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

