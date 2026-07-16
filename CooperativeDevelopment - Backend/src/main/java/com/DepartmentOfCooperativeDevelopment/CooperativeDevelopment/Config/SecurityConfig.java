package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
                    corsConfiguration.setAllowedOrigins(List.of("https://personalfile-frontend.onrender.com"));
                    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                }))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/departments/all").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        .requestMatchers("/departments/add").hasRole("PERSONALFILE_ADMIN")

                        .requestMatchers("/leave/**").hasAnyRole("PERSONALFILE_ADMIN", "EMPLOYEE")

                        .requestMatchers("/dynamic-fields/**").hasAnyRole("PERSONALFILE_ADMIN", "EMPLOYEE")

                        .requestMatchers("/drivers/**").hasAnyRole("VEHICLE_ADMIN", "VEHICLE_APPROVAL", "EMPLOYEE", "DRIVER")
                        .requestMatchers("/vehicles/**").hasAnyRole("VEHICLE_ADMIN", "VEHICLE_APPROVAL", "EMPLOYEE", "DRIVER")

                        .requestMatchers("/increment-form/**").hasAnyRole("PERSONALFILE_ADMIN", "EMPLOYEE")

                        .requestMatchers("/vehicle-requests/**").hasAnyRole("VEHICLE_ADMIN", "VEHICLE_APPROVAL", "EMPLOYEE", "DRIVER")

                        .requestMatchers("/personalfile/update-profile/**").hasAnyRole("PERSONALFILE_ADMIN", "EMPLOYEE")
                        .requestMatchers("/personalfile/upload-employees").hasRole("PERSONALFILE_ADMIN")
                        .requestMatchers("/personalfile/all-employees").hasAnyRole("PERSONALFILE_ADMIN", "VEHICLE_ADMIN")
                        .requestMatchers("/personalfile/me").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}