package residencia.gestao_condominio.controllers;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import residencia.gestao_condominio.dto.LoginRequest;
import residencia.gestao_condominio.dto.LoginResponse;
import residencia.gestao_condominio.services.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        return authService.login(body);
    }
}
