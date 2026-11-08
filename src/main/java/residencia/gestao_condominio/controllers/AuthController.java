package residencia.gestao_condominio.controllers;

import jakarta.servlet.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import residencia.gestao_condominio.entities.*;
import residencia.gestao_condominio.exceptions.SecurityErrors;
import residencia.gestao_condominio.repositories.*;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository users;
    private final UnitBindingRepository bindings;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String dummyHash;
    public AuthController(UserRepository users, UnitBindingRepository bindings, PasswordEncoder passwords, JwtEncoder encoder) {
        this.users = users; this.bindings = bindings; this.passwords = passwords; this.encoder = encoder;
        this.dummyHash = passwords.encode("invalid-user-timing-check");
    }
    // condoId is optional when the resident has exactly one condominium.
    public record LoginRequest(String cpf, String password, Long condoId) {}
    @PostMapping("/login")
    @Transactional(readOnly = true)
    public Map<String, Object> login(@RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) throws IOException {
        var user = body.cpf() == null ? null : users.findByCpf(body.cpf()).orElse(null);
        if (body.password() == null || body.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
            || !passwords.matches(body.password(), user == null ? dummyHash : user.getPassword()) || user == null) {
            SecurityErrors.write(request, response, 401); return null;
        }
        Long condo = null;
        String role;
        if (user instanceof Administrator admin) {
            role = "MANAGER"; condo = admin.getCondominium().getId();
            if (body.condoId() != null && !condo.equals(body.condoId())) condo = null;
        } else {
            role = "RESIDENT";
            var condos = bindings.findAllByResidentId(user.getId()).stream().map(b -> b.getUnit().getCondominium().getId()).distinct().toList();
            if (body.condoId() != null && condos.contains(body.condoId())) condo = body.condoId();
            else if (body.condoId() == null && condos.size() == 1) condo = condos.getFirst();
        }
        if (condo == null) { SecurityErrors.write(request, response, 403); return null; }
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(user.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(7200))
            .claim("role", role).claim("condoId", condo).build();
        var token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return Map.of("token", token, "expiresIn", 7200, "role", role, "condoId", condo);
    }
}
