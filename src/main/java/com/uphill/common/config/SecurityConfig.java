package com.uphill.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(value = "app.auth.enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain disableSecurity(HttpSecurity http) {
        http.cors(AbstractHttpConfigurer::disable).csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(registry -> registry.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(value = "app.auth.enabled", havingValue = "true")
    public SecurityFilterChain enableSecurity(HttpSecurity http, @Value("${app.auth.jwk_endpoint}") String jwkEndpoint) {

        http.cors(AbstractHttpConfigurer::disable).csrf(AbstractHttpConfigurer::disable);

        http.sessionManagement(httpSecuritySessionManagementConfigurer
                -> httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(registry -> {
            registry.requestMatchers("/").permitAll();
            registry.requestMatchers("/actuator/**").permitAll();
            registry.requestMatchers("/swagger-ui.html").permitAll();
            registry.requestMatchers("/swagger-ui/*").permitAll();
            registry.requestMatchers("/error").permitAll();
            registry.requestMatchers("/v3/**").permitAll();
            registry.anyRequest().authenticated();
        });

        http.oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer
                -> httpSecurityOAuth2ResourceServerConfigurer.jwt(jwtConfigurer
                -> jwtConfigurer.jwkSetUri(jwkEndpoint)));

        return http.build();
    }
}
