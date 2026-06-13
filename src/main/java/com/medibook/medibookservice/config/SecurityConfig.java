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

/**
 * Spring Security configuration for the MediBook application.
 *
 * <p><b>Security Strategy:</b></p>
 * <ul>
 *   <li>JWT-based stateless authentication</li>
 *   <li>Public endpoints explicitly permit-all'd</li>
 *   <li>All other endpoints require authentication (catch-all)</li>
 *   <li>Role-based access enforced via @PreAuthorize on controllers</li>
 * </ul>
 *
 * <p><b>Phase Evolution:</b></p>
 * <ul>
 *   <li>Phase 1: Authentication endpoints public; rest authenticated</li>
 *   <li>Phase 2: Doctor browsing endpoints (GET) added as public</li>
 *   <li>Phase 3: Slot browsing endpoints (GET) added as public</li>
 *   <li>Phase 4: Booking endpoints - NO public access (private data)</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
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
                        // PHASE 1: PUBLIC ENDPOINTS (authentication & monitoring)
                        // =====================================================
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // =====================================================
                        // PHASE 2: PUBLIC DOCTOR BROWSING (GET only, numeric IDs)
                        // =====================================================
                        .requestMatchers(HttpMethod.GET, "/doctors").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/specializations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}").permitAll()

                        // =====================================================
                        // PHASE 3: PUBLIC SLOT BROWSING (GET only, numeric IDs)
                        // =====================================================
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}/slots/available").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}/slots/available/range").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}/slots/available/next").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctors/{id:[0-9]+}/slots/dates").permitAll()

                        // =====================================================
                        // PHASE 4: BOOKING ENDPOINTS (NONE public - all require auth)
                        // =====================================================
                        // Bookings contain private patient data. Always authenticated.
                        // Role enforcement via @PreAuthorize on controllers:
                        //
                        //   PatientBookingController  -> @PreAuthorize("hasRole('PATIENT')")
                        //     Paths: /me/bookings/**
                        //
                        //   DoctorBookingController   -> @PreAuthorize("hasRole('DOCTOR')")
                        //     Paths: /doctors/me/bookings/**
                        //
                        //   AdminBookingController    -> @PreAuthorize("hasRole('ADMIN')")
                        //     Paths: /admin/bookings/**
                        //            /admin/patients/{id}/bookings
                        //            /admin/doctors/{id}/bookings

                        // =====================================================
                        // CATCH-ALL: everything else requires authentication
                        // =====================================================
                        // Also covers:
                        //   /doctors/me/**         -> DOCTOR role (Phase 2, 3, 4)
                        //   /admin/**              -> ADMIN role (Phase 2, 3, 4)
                        //   /me/**                 -> PATIENT role (Phase 4)
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