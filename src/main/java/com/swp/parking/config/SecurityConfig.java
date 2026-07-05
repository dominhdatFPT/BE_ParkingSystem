package com.swp.parking.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origin-patterns}")
    private List<String> allowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Cross-Origin-Opener-Policy", "same-origin-allow-popups"))
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // VNPay webhooks — phải public vì VNPay server gọi trực tiếp (không có JWT)
                        .requestMatchers(HttpMethod.GET, "/api/payments/vnpay/ipn").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/payments/vnpay/return").permitAll()
                        // VNPay order status — FE polling sau redirect, có thể không còn session hợp lệ
                        .requestMatchers(HttpMethod.GET, "/api/payments/vnpay/orders/*/status").permitAll()

                        // Stripe webhook — Stripe server gọi trực tiếp, không có JWT
                        .requestMatchers(HttpMethod.POST, "/api/payments/stripe/webhook").permitAll()
                        // Stripe order status — FE polling sau confirmCardPayment
                        .requestMatchers(HttpMethod.GET, "/api/payments/stripe/orders/*/status").permitAll()
                        // Stripe confirm — FE gọi ngay sau success để kích hoạt subscription (fallback webhook)
                        .requestMatchers(HttpMethod.POST, "/api/payments/stripe/orders/*/confirm").permitAll()

                        // Dev/test endpoints — mock VNPay (không active trên prod do @Profile)
                        .requestMatchers("/api/dev/**").permitAll()

                        // Public read endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/parking-area-summary/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/fee-packages", "/api/v1/fee-packages/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/notifications", "/api/v1/notifications/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicle-types").permitAll()

                        // User-only endpoints (must be checked before generic admin/staff rules)
                        .requestMatchers(HttpMethod.POST, "/api/v1/vehicle-registrations").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicle-registrations/my").hasRole("USER")
                        // Admin/Staff management endpoints
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/staff/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/users").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/parking-slots", "/api/v1/parking-slots/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/parking-entry", "/api/v1/parking-entry/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/parking-exit", "/api/v1/parking-exit/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/incidents", "/api/v1/incidents/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/audit-logs").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/system-configuration").hasAnyRole("ADMIN", "STAFF")

                        // Fee package management
                        .requestMatchers(HttpMethod.POST, "/api/v1/fee-packages").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/fee-packages/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/fee-packages/**").hasAnyRole("ADMIN", "STAFF")

                        // Vehicle registrations management
                        .requestMatchers(HttpMethod.POST, "/api/v1/vehicle-registrations/users/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicle-registrations").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicle-registrations/pending").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/vehicle-registrations/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicle-registrations/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/vehicle-registrations/**").hasAnyRole("ADMIN", "STAFF")

                        // Customer endpoints
                        .requestMatchers("/api/customer/support/**").hasRole("USER")
                        .requestMatchers("/api/customer/**").authenticated()

                        .anyRequest().authenticated()
                )
                // Trả 401 (không phải 403) khi chưa đăng nhập / token hết hạn
                // → FE mới có thể trigger refresh-token tự động
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(
                                    "{\"status\":401,\"message\":\"Unauthorized – vui lòng đăng nhập lại\"}"
                            );
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
