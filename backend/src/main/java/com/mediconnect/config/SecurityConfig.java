package com.mediconnect.config;

import com.mediconnect.enums.AccountStatus;
import com.mediconnect.enums.UserRole;
import com.mediconnect.enums.VerificationStatus;
import com.mediconnect.repository.UserRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, UserRepository users) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            String message = exception instanceof org.springframework.security.web.csrf.CsrfException
                                    ? "Your security token is missing or expired. Refresh the page and try again."
                                    : "You do not have permission to access this resource.";
                            response.getWriter().write("{\"message\":\"" + message + "\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        // Let the original error status reach the client instead of re-authenticating
                        // the internal ERROR dispatch and disguising application failures as 401/403.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/register/**",
                                "/api/auth/setup-admin",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/doctors",
                                "/api/doctors/*",
                                "/api/doctors/*/availability",
                                "/api/labs/**",
                                "/api/medicines/**",
                                "/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").access((authentication, context) ->
                                new AuthorizationDecision(hasActiveRole(users, authentication.get(), UserRole.ADMIN)))
                        .requestMatchers("/api/doctor/**").access((authentication, context) ->
                                new AuthorizationDecision(hasActiveRole(users, authentication.get(), UserRole.DOCTOR)))
                        .requestMatchers("/api/pharmacy/**").access((authentication, context) ->
                                new AuthorizationDecision(hasActiveRole(users, authentication.get(), UserRole.PHARMACY)))
                        .requestMatchers("/api/laboratory/**").access((authentication, context) ->
                                new AuthorizationDecision(hasActiveRole(users, authentication.get(), UserRole.LABORATORY)))
                        .requestMatchers("/api/patient/**").access((authentication, context) ->
                                new AuthorizationDecision(hasActiveRole(users, authentication.get(), UserRole.PATIENT)))
                        .requestMatchers("/api/appointments/**", "/api/profile/**").authenticated()
                        .requestMatchers("/api/dashboard").authenticated()
                        .anyRequest().authenticated())
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session
                        .sessionFixation(fix -> fix.migrateSession())
                        .maximumSessions(1))
                .build();
    }

    private static boolean hasActiveRole(UserRepository users,
                                         org.springframework.security.core.Authentication authentication,
                                         UserRole role) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return users.findByEmailIgnoreCase(authentication.getName())
                .filter(user -> user.isActive()
                        && user.isEmailVerified()
                        && user.getAccountStatus() == AccountStatus.ACTIVE
                        && user.getRole() == role
                        && user.getVerificationStatus() != VerificationStatus.PENDING
                        && user.getVerificationStatus() != VerificationStatus.REJECTED)
                .isPresent();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origin}") String origin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(origin));
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-Setup-Token", "X-XSRF-TOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
