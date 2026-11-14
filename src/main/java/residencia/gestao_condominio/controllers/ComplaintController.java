package residencia.gestao_condominio.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import residencia.gestao_condominio.dto.ComplaintDetailResponse;
import residencia.gestao_condominio.dto.ComplaintReceipt;
import residencia.gestao_condominio.dto.ActionResponse;
import residencia.gestao_condominio.dto.CreateActionRequest;
import residencia.gestao_condominio.dto.UpdateComplaintStatusRequest;
import residencia.gestao_condominio.dto.ComplaintSummaryResponse;
import residencia.gestao_condominio.dto.CreateComplaintRequest;
import residencia.gestao_condominio.dto.PageResponse;
import residencia.gestao_condominio.services.ComplaintService;
import residencia.gestao_condominio.security.ProtocolLookupLimiter;
import residencia.gestao_condominio.dto.ComplaintTrackingResponse;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {
    private final ComplaintService complaints;
    private final ProtocolLookupLimiter limiter;

    public ComplaintController(ComplaintService complaints, ProtocolLookupLimiter limiter) {
        this.complaints = complaints;
        this.limiter = limiter;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintReceipt register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateComplaintRequest request) {
         return complaints.register(condoId(jwt), request);
    }

     @GetMapping("/protocol/{protocol}")
    public ComplaintTrackingResponse track(@PathVariable String protocol, HttpServletRequest request) {
         limiter.check(request.getRemoteAddr());
        return complaints.trackByProtocol(protocol);
    }

     @GetMapping
    public PageResponse<ComplaintSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return complaints.listForManager(condoId(jwt), page, size);
    }

    @GetMapping("/{id}")
    public ComplaintDetailResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return complaints.detailForManager(condoId(jwt), id);
    }

    @PatchMapping("/{id}/status")
    public ComplaintTrackingResponse.StatusHistoryItem updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                                   @Valid @RequestBody UpdateComplaintStatusRequest request) {
        return complaints.updateStatus(condoId(jwt), id, request);
    }

    @PostMapping("/{id}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public ActionResponse registerAction(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @Valid @RequestBody CreateActionRequest request) {
        return complaints.registerAction(condoId(jwt), id, Long.valueOf(jwt.getSubject()), request);
    }

    private Long condoId(Jwt jwt) {
        return jwt.<Number>getClaim("condoId").longValue();
    }
}
