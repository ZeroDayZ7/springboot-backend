package com.app.backend.config;

import java.util.List;
import org.springframework.security.config.Customizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@Configuration
public class SecurityConfig {

        private final CorsProperties corsProperties;

        public SecurityConfig(CorsProperties corsProperties) {
                this.corsProperties = corsProperties;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.ignoringRequestMatchers(toH2Console()))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .headers(headers -> headers
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000))
                                                .contentTypeOptions(Customizer.withDefaults())
                                                // .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                                                .frameOptions(frame -> frame.sameOrigin())
                                                .referrerPolicy(
                                                                referrer -> referrer.policy(
                                                                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                                                .permissionsPolicyHeader(header -> header
                                                                .policy("geolocation=(), microphone=(), camera=()")))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(toH2Console()).permitAll()
                                                .anyRequest().permitAll());

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(true);
                config.setAllowedOrigins(corsProperties.getAllowedOrigins());
                // config.setAllowedOriginPatterns(List.of("*"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "Origin",
                                "X-Requested-With"));
                // config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                // source.registerCorsConfiguration("/**", config);
                source.registerCorsConfiguration("/h2-console/**", config);
                return source;
        }
}
