package com.bankms.config;

import com.bankms.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.Collections.singletonList("*"));
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String role = authority.getAuthority();
                    if ("ROLE_ADMIN".equals(role)) {
                        response.sendRedirect(request.getContextPath() + "/dashboard/admin");
                        return;
                    } else if ("ROLE_EMPLOYEE".equals(role)) {
                        response.sendRedirect(request.getContextPath() + "/dashboard/employee");
                        return;
                    } else if ("ROLE_CUSTOMER".equals(role)) {
                        response.sendRedirect(request.getContextPath() + "/dashboard/customer");
                        return;
                    }
                }
                response.sendRedirect(request.getContextPath() + "/dashboard");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Static resources & public endpoints
                .requestMatchers("/css/**", "/js/**", "/webjars/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/", "/login", "/register", "/forgot-password", "/reset-password", "/error/**", "/h2-console/**", "/api/**").permitAll()

                // Admin-only endpoints
                .requestMatchers("/dashboard/admin").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Employee & Admin endpoints
                .requestMatchers("/dashboard/employee").hasRole("EMPLOYEE")
                .requestMatchers("/customers/new", "/customers/edit/**", "/customers/delete/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/accounts/new", "/accounts/status/**").hasAnyRole("ADMIN", "EMPLOYEE")
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "EMPLOYEE")

                // Customer dashboard
                .requestMatchers("/dashboard/customer").hasRole("CUSTOMER")

                // Shared authenticated endpoints with method / service-level authorization
                .requestMatchers("/dashboard").authenticated()
                .requestMatchers("/customers/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                .requestMatchers("/accounts/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")
                .requestMatchers("/transactions/**").hasAnyRole("ADMIN", "EMPLOYEE", "CUSTOMER")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(roleBasedAuthenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // Allow H2 console frames if enabled
            );

        return http.build();
    }
}
