package residencia.gestao_condominio.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import residencia.gestao_condominio.exceptions.SecurityErrors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.security.SecureRandom;
import java.time.Duration;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean SecretKey jwtKey(@Value("${app.security.jwt.secret:}") String secret) {
        byte[] bytes;
        if (secret.isBlank()) {
            // Ephemeral local key: restart invalidates existing tokens.
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = Base64.getDecoder().decode(secret);
            if (bytes.length < 32) throw new IllegalArgumentException("JWT secret must contain at least 32 random bytes (Base64)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build(); }
    @Bean JwtDecoder jwtDecoder(SecretKey key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
        return decoder;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, ApiAuthorization authorization) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .authorizeHttpRequests(requests -> requests
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/login", "/api/v1/condominiums").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/complaints/protocol/*").permitAll()
                .anyRequest().access(authorization))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, ex) -> SecurityErrors.write(req, res, 401))
                .accessDeniedHandler((req, res, ex) -> SecurityErrors.write(req, res, 403)))
            .oauth2ResourceServer(resource -> resource.jwt(jwt -> {})
                .authenticationEntryPoint((req, res, ex) -> SecurityErrors.write(req, res, 401))
                .accessDeniedHandler((req, res, ex) -> SecurityErrors.write(req, res, 403)))
            .build();
    }
}
