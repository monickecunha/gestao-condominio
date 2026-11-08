package residencia.gestao_condominio.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import residencia.gestao_condominio.dto.BindingRequest;
import residencia.gestao_condominio.dto.BindingResponse;
import residencia.gestao_condominio.services.BindingService;

@RestController
@RequestMapping("/api/v1/bindings")
public class BindingController {
    private final BindingService bindings;

    public BindingController(BindingService bindings) {
        this.bindings = bindings;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BindingResponse register(@Valid @RequestBody BindingRequest request) {
        return bindings.register(request);
    }
}
