package ru.astrakhan.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.astrakhan.admin.security.ApiJsonAuthHandlers;
import ru.astrakhan.admin.security.ClientJwtAuthenticationFilter;

@Configuration
public class ApiSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            ClientJwtAuthenticationFilter clientJwtAuthenticationFilter,
            ApiJsonAuthHandlers apiJsonAuthHandlers) throws Exception {
        http
                .securityMatcher("/api/v1/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(apiJsonAuthHandlers)
                        .accessDeniedHandler(apiJsonAuthHandlers))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/profile/login", "/api/v1/profile/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/internal/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/routes/*/complete").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/rewards/redeem").authenticated()
                        .requestMatchers("/api/v1/user-progress", "/api/v1/user-progress/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(clientJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
