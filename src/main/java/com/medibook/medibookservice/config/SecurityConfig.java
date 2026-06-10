package com.medibook.medibookservice.config;

import com.medibook.medibookservice.security.CustomUserDetailsService;
import com.medibook.medibookservice.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity   // Enables @PreAuthorize for role-based access on methods
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // =====================================================
                        // FULLY PUBLIC ENDPOINTS (no auth required)
                        // =====================================================
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Public doctor browsing - GET requests only
                        .requestMatchers(HttpMethod.GET, "/doctors").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/specializations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}").permitAll()

                        // Public slot browsing (Phase 3) - only numeric IDs
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}/slots/available").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}/slots/available/range").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}/slots/available/next").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}/slots/dates").permitAll()

                        // =====================================================
                        // AUTHENTICATED ENDPOINTS
                        // =====================================================
                        // Note: /doctors/me/** are handled by @PreAuthorize on controller
                        // Note: /admin/** are handled by @PreAuthorize on controller
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}