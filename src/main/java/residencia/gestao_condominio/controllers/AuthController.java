package residencia.gestao_condominio.controllers;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import residencia.gestao_condominio.dto.LoginRequest;
import residencia.gestao_condominio.dto.LoginResponse;
import residencia.gestao_condominio.exceptions.SecurityErrors;
import residencia.gestao_condominio.services.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest body) {
        return authService.login(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public void invalidCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityErrors.write(request, response, HttpServletResponse.SC_UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void accessDenied(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityErrors.write(request, response, HttpServletResponse.SC_FORBIDDEN);
    }
}
