package com.bright.wallet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CorsConfig allows the Vue frontend (localhost:5173)
 * to make HTTP requests to the Spring Boot backend (localhost:8082).
 *
 * Without this, the browser blocks every request with:
 * "No 'Access-Control-Allow-Origin' header is present"
 *
 * CORS (Cross-Origin Resource Sharing) is a browser security rule —
 * it blocks requests from a different origin (different port = different origin)
 * unless the server explicitly says "I allow requests from that origin."
 *
 * Lives in: com.bright.wallet.config
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow requests from the Vue dev server
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:4173",   // Vite preview server
                "http://nyukzin-wallet-frontend.s3-website.af-south-1.amazonaws.com",
                "https://d20lo4xu4wwlof.cloudfront.net"
        ));

        // Allow all standard HTTP methods
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allow the Authorization header (JWT token) and Content-Type
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));

        // Allow credentials (needed for Authorization header)
        config.setAllowCredentials(true);

        // How long the browser caches the preflight response (1 hour)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply this config to ALL endpoints
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}