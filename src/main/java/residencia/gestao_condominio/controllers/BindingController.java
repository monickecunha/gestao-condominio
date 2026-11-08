package residencia.gestao_condominio.controllers;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import residencia.gestao_condominio.dto.BindingListFilter;
import residencia.gestao_condominio.dto.PendingBindingResponse;
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

    @GetMapping
    public List<PendingBindingResponse> listPending(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @ModelAttribute BindingListFilter filter) {
        return bindings.listPending(jwt.<Number>getClaim("condoId").longValue());
    }

    @PostMapping("/{bindingId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable Long bindingId, @AuthenticationPrincipal Jwt jwt) {
        bindings.approve(jwt.<Number>getClaim("condoId").longValue(), bindingId);
    }

    @PostMapping("/{bindingId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long bindingId, @AuthenticationPrincipal Jwt jwt) {
        bindings.reject(jwt.<Number>getClaim("condoId").longValue(), bindingId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BindingResponse register(@Valid @RequestBody BindingRequest request) {
        return bindings.register(request);
    }
}
