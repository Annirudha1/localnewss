package com.localnews.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // Public pages - no authentication required
                        .requestMatchers("/", "/login", "/register", "/home", "/css/**", "/js/**", "/images/**").permitAll()

                        // Mobile login and OTP authentication - public access
                        .requestMatchers("/api/auth/**", "/api/auth/send-otp", "/api/auth/verify-otp", "/api/auth/districts").permitAll()

                        // Video feed and user pages - accessible after OTP login
                        .requestMatchers("/video-feed", "/mobile-login").permitAll()

                        // Admin pages - public access for now (can be secured later)
                        .requestMatchers("/admin", "/admin/**", "/api/admin/**").permitAll()

                        // API endpoints for video platform - public for testing
                        .requestMatchers("/api/videos/**", "/api/media/**").permitAll()

                        // File uploads and static resources
                        .requestMatchers("/upload", "/uploads/**", "/logout").permitAll()

                        // Health check
                        .requestMatchers("/health", "/actuator/health").permitAll()

                        // Everything else requires authentication (can be relaxed for testing)
                        .anyRequest().permitAll() // Changed to permitAll for testing
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Allow sessions for UI
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false))
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
