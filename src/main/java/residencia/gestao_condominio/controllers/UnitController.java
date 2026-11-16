package residencia.gestao_condominio.controllers;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import residencia.gestao_condominio.dto.CreateUnitRequest;
import residencia.gestao_condominio.dto.UnitResponse;
import residencia.gestao_condominio.services.UnitService;

@RestController
@RequestMapping("/api/v1/condominiums/{condoId}/units")
public class UnitController {
    private final UnitService units;

    public UnitController(UnitService units) {
        this.units = units;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnitResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUnitRequest request) {
        return units.create(condominiumId(jwt), request);
    }

    @GetMapping
    public List<UnitResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return units.list(condominiumId(jwt));
    }

    private Long condominiumId(Jwt jwt) {
        return jwt.<Number>getClaim("condoId").longValue();
    }
}
