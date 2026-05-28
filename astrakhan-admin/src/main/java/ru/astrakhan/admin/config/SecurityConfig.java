package ru.astrakhan.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import ru.astrakhan.admin.service.AdminUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] STAFF_ROLES = {"ADMIN", "USER", "MODERATOR", "EDITOR"};

    private final AdminUserDetailsService adminUserDetailsService;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    public SecurityConfig(AdminUserDetailsService adminUserDetailsService) {
        this.adminUserDetailsService = adminUserDetailsService;
    }

    /** Сессия + form login для админки и служебных /admin/api/*. */
    @Bean
    @Order(2)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/login", "/css/**", "/js/**", "/error", "/favicon.ico", "/favicon.svg").permitAll();
                    auth.requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**"
                    ).permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.requestMatchers("/admin/api/v1/**").hasAnyRole(STAFF_ROLES);
                    auth.requestMatchers("/admin/users", "/admin/users/**").hasAnyRole(STAFF_ROLES);
                    auth.requestMatchers("/admin/**").hasAnyRole(STAFF_ROLES);
                    auth.anyRequest().permitAll();
                })
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin", true)
                        .failureUrl("/login?error")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                )
                .userDetailsService(adminUserDetailsService);

        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/admin/api/**"));
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
