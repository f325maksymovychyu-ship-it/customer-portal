package org.example.customerportal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Security wiring for US-001 (security-conventions.md SC-1 / SC-4 / SC-5,
 * API design §6, OD-002:B, risk R-2).
 *
 * <ul>
 *   <li>{@code POST /api/v1/customers} is the single public route; every other
 *       request requires authentication (deny-by-default, SC-4).</li>
 *   <li>CSRF stays enabled globally and is exempted <em>only</em> for
 *       {@code POST /api/v1/customers} (OD-002:B — the recorded SC-5 architecture
 *       decision).</li>
 *   <li>Unauthenticated access to a protected route returns {@code 401}
 *       (JSON-API posture) rather than a form-login redirect.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    /** Matches exactly {@code POST /api/v1/customers} — used for both the public rule and the CSRF exemption. */
    private static final RequestMatcher REGISTRATION_ENDPOINT = request ->
            "POST".equalsIgnoreCase(request.getMethod())
                    && "/api/v1/customers".equals(request.getRequestURI());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(REGISTRATION_ENDPOINT).permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(REGISTRATION_ENDPOINT))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
