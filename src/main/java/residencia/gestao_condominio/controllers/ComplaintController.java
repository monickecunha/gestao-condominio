package residencia.gestao_condominio.controllers;

import jakarta.servlet.http.HttpServletRequest; //mateus F08
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import residencia.gestao_condominio.dto.ComplaintReceipt;
import residencia.gestao_condominio.dto.CreateComplaintRequest;
import residencia.gestao_condominio.services.ComplaintService;
import residencia.gestao_condominio.security.ProtocolLookupLimiter; //mateus F08
import residencia.gestao_condominio.dto.ComplaintTrackingResponse; // mateus F08

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {
    private final ComplaintService complaints;
    private final ProtocolLookupLimiter limiter; //mateus F08

    public ComplaintController(ComplaintService complaints, ProtocolLookupLimiter limiter) {
        this.complaints = complaints;
        this.limiter = limiter; //mateus F08
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintReceipt register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateComplaintRequest request) {
        return complaints.register(jwt.<Number>getClaim("condoId").longValue(), request);
    }

     @GetMapping("/protocol/{protocol}")//mateus F08
    public ComplaintTrackingResponse track(@PathVariable String protocol, HttpServletRequest request) {
         limiter.check(request.getRemoteAddr());   //mateus F08
        return complaints.trackByProtocol(protocol);
    }
}
