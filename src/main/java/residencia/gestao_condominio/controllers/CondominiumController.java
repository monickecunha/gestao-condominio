package residencia.gestao_condominio.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import residencia.gestao_condominio.dto.CondominiumRegistrationRequest;
import residencia.gestao_condominio.dto.CondominiumRegistrationResponse;
import residencia.gestao_condominio.services.CondominiumRegistrationService;

@RestController
@RequestMapping("/api/v1/condominiums")
public class CondominiumController {
    private final CondominiumRegistrationService registration;

    public CondominiumController(CondominiumRegistrationService registration) {
        this.registration = registration;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CondominiumRegistrationResponse register(@Valid @RequestBody CondominiumRegistrationRequest request) {
        return registration.register(request);
    }
}
