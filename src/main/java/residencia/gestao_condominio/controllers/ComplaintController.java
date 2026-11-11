package residencia.gestao_condominio.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import residencia.gestao_condominio.dto.ComplaintReceipt;
import residencia.gestao_condominio.dto.CreateComplaintRequest;
import residencia.gestao_condominio.services.ComplaintService;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {
    private final ComplaintService complaints;

    public ComplaintController(ComplaintService complaints) {
        this.complaints = complaints;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintReceipt register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateComplaintRequest request) {
        return complaints.register(jwt.<Number>getClaim("condoId").longValue(), request);
    }
}
