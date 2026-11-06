package com.maonaroda.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http, SessaoAtivaFilter.VerificadorSessao sessoes) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/api/v1/perfis-mobilidade").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/rotas", "/api/v1/rotas/base",
                                "/api/v1/destinos/busca", "/api/v1/auth/cadastro", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/usuarios/**", "/api/v1/relatos/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
                // Depois de validar o JWT, recusa tokens cuja sessão já terminou (RNF16).
                .addFilterAfter(new SessaoAtivaFilter(sessoes), BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${auth.issuer:}") String issuer, @Value("${auth.jwk-set-uri:}") String jwks, @Value("${auth.audience:authenticated}") String audience) {
        // Sem configuração, as operações públicas funcionam e todo token é recusado.
        if (issuer.isBlank() || jwks.isBlank()) return token -> {
            throw new BadJwtException("Autenticação não configurada.");
        };

        if (!issuer.startsWith("https://") || !jwks.startsWith("https://"))
            throw new IllegalArgumentException("Emissor e JWKS de produção exigem HTTPS.");
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        decoder.setJwtValidator(validadores(issuer, audience));
        return decoder;
    }

    public static OAuth2TokenValidator<Jwt> validadores(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> aud = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));

        OAuth2TokenValidator<Jwt> duracao = jwt -> jwt.getIssuedAt() != null && jwt.getExpiresAt() != null
                && jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                && !jwt.getExpiresAt().isAfter(jwt.getIssuedAt().plusSeconds(900))
                && !jwt.getIssuedAt().isAfter(java.time.Instant.now().plusSeconds(60))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));

        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(java.time.Duration.ZERO),
                new JwtIssuerValidator(issuer), aud, duracao);
    }
}
