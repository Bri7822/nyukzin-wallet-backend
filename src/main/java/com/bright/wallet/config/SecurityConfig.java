package com.bright.wallet.config;

import com.bright.wallet.security.CustomUserDetailsService;
import com.bright.wallet.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter                jwtFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfig               corsConfig;

    public SecurityConfig(JwtFilter jwtFilter,
                          CustomUserDetailsService userDetailsService,
                          CorsConfig corsConfig) {
        this.jwtFilter          = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfig         = corsConfig;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Unauthorized. Please login and include your token.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Access denied. Admins only.\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // ── Public — no token needed ──────────────────────────
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/users",
                                "/api/payments/webhook"     // Paystack webhook — HMAC-SHA512-verified, no JWT
                        ).permitAll()

                        // ── Admin only ────────────────────────────────────────
                        .requestMatchers("/auth/register-admin").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ── Everything else — authenticated users ─────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}