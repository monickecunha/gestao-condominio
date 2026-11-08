package residencia.gestao_condominio.services;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import residencia.gestao_condominio.dto.LoginRequest;
import residencia.gestao_condominio.dto.LoginResponse;
import residencia.gestao_condominio.entities.Administrator;
import residencia.gestao_condominio.entities.User;
import residencia.gestao_condominio.repositories.UnitBindingRepository;
import residencia.gestao_condominio.repositories.UserRepository;

@Service
public class AuthService {
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(2);

    private final UserRepository users;
    private final UnitBindingRepository bindings;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String dummyHash;

    public AuthService(UserRepository users, UnitBindingRepository bindings, PasswordEncoder passwords, JwtEncoder encoder) {
        this.users = users;
        this.bindings = bindings;
        this.passwords = passwords;
        this.encoder = encoder;
        this.dummyHash = passwords.encode("invalid-user-timing-check");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = authenticate(request.cpf(), request.password());
        String role;
        List<Long> condoIds;
        if (user instanceof Administrator admin) {
            role = "MANAGER";
            condoIds = List.of(admin.getCondominium().getId());
        } else {
            role = "RESIDENT";
            condoIds = bindings.findAllByResidentId(user.getId()).stream()
                    .map(binding -> binding.getUnit().getCondominium().getId())
                    .distinct()
                    .toList();
        }

        Long condoId = selectCondominium(condoIds, request.condoId());
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_VALIDITY))
                .claim("role", role)
                .claim("condoId", condoId)
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, TOKEN_VALIDITY.toSeconds(), role, condoId);
    }

    private User authenticate(String cpf, String password) {
        User user = cpf == null ? null : users.findByCpf(cpf).orElse(null);
        String hash = user == null ? dummyHash : user.getPassword();
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwords.matches(password, hash) || user == null) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return user;
    }

    private Long selectCondominium(List<Long> condoIds, Long requestedId) {
        if (requestedId != null && condoIds.contains(requestedId)) {
            return requestedId;
        }
        if (requestedId == null && condoIds.size() == 1) {
            return condoIds.getFirst();
        }
        throw new AccessDeniedException("Access denied");
    }
}
