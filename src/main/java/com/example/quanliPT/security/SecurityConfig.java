package com.example.quanliPT.security;

import com.example.quanliPT.model.*;
import com.example.quanliPT.repository.auth.*;
import com.example.quanliPT.repository.user.*;
import com.example.quanliPT.repository.room.*;
import com.example.quanliPT.repository.finance.*;
import com.example.quanliPT.repository.contract.*;
import com.example.quanliPT.repository.notification.*;
import com.example.quanliPT.repository.guest.*;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configure(http))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // Cho phép PayOS Webhook gọi về không cần JWT
                        .requestMatchers(HttpMethod.POST, "/api/payments/payos/callback").permitAll()
                        // Cho phép truy cập công khai vào API đăng ký thuê phòng (POST)
                        .requestMatchers(HttpMethod.POST, "/api/public/rental-requests").permitAll()
                        // Cho phép truy cập công khai vào các API phòng (GET)
                        .requestMatchers(HttpMethod.GET, "/api/rooms/**").permitAll()
                        // Cho phép truy cập công khai vào các API khác trong /api/public/
                        .requestMatchers("/api/public/**").permitAll()
                        // Cho phép WebSocket endpoint
                        .requestMatchers("/ws/**").permitAll()
                        // Swagger UI
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        
                        // Admin-only management
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/rooms/**").hasRole("ADMIN")
                        
                        // Management endpoints
                        .requestMatchers("/api/contracts/**").hasAnyRole("ADMIN", "TENANT")
                        .requestMatchers("/api/invoices/**").hasAnyRole("ADMIN", "TENANT")
                        .requestMatchers("/api/maintenance/**").hasAnyRole("ADMIN", "TENANT")
                        
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}


