package dev.robgro.timesheet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.header.writers.PermissionsPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XContentTypeOptionsHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationSuccessHandler successHandler;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080", "https://timesheet.robgro.dev",
                "https://robgro.dev", "null"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' " +
                                        "https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; style-src 'self' " +
                                        "'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                                        "img-src 'self' data:;")
                        )
                        .xssProtection(HeadersConfigurer.XXssConfig::disable)
                        .frameOptions(frame -> frame.sameOrigin())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .addHeaderWriter(new XXssProtectionHeaderWriter())
                        .addHeaderWriter(new XContentTypeOptionsHeaderWriter())
                        .addHeaderWriter(new ReferrerPolicyHeaderWriter(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .addHeaderWriter(new PermissionsPolicyHeaderWriter("geolocation=(self), microphone=()"))
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                            "/api/auth/login", // JWT login (stateless)
                            "/api/auth/forgot-password", // Password reset request
                            "/api/auth/reset-password",  // Password reset submit
                            "/api/track/**",             // Email tracking (external clients)
                            "/login"
                        )
                )

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Exception handling - return 403 for API endpoints instead of redirect
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.FORBIDDEN),
                                new AntPathRequestMatcher("/api/**")
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - Web UI
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/forgot-password", "/reset-password").permitAll() // Password reset flow
                        .requestMatchers("/manifest/**", "/icons/**").permitAll()

                        // Public endpoints - API (explicit, no wildcards)
                        .requestMatchers("/api/auth/login").permitAll() // Only JWT login is public
                        .requestMatchers("/api/auth/forgot-password").permitAll() // Password reset request
                        .requestMatchers("/api/auth/reset-password").permitAll() // Password reset submit
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Email tracking: GET-only enforcement (CRITICAL SECURITY)
                        .requestMatchers(HttpMethod.GET, "/api/track/**").permitAll()
                        .requestMatchers("/api/track/**").denyAll() // Block POST/PUT/DELETE/PATCH

                        // API endpoints - access levels
                        .requestMatchers("/api/v1/clients/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/v1/invoices/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/v1/timesheets/**").hasAnyRole("ADMIN", "USER", "GUEST")

                        // Web UI endpoints
                        .requestMatchers("/clients/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/invoices/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/timesheets/**").hasAnyRole("ADMIN", "USER", "GUEST")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                // API JWT config + session tracking
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(10)  // Allow multiple sessions per user
                                .sessionRegistry(sessionRegistry())
                                .maxSessionsPreventsLogin(false)  // Allow new login, expire oldest
                                .expiredUrl("/login?expired=true")  // Redirect when session expired
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider())
                .build();
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * SessionRegistry for tracking active user sessions.
     * Required for session invalidation on password reset.
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * HttpSessionEventPublisher for SessionRegistry to work correctly.
     * CRITICAL: Without this, SessionRegistry won't track sessions.
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
