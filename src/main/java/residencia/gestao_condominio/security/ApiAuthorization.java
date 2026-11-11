package residencia.gestao_condominio.security;

import org.springframework.security.authorization.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import residencia.gestao_condominio.repositories.*;
import residencia.gestao_condominio.entities.BindingStatus;
import java.util.function.Supplier;

/** Deny by default. Resource IDs are resolved through condominium-scoped queries. */
@Component
public class ApiAuthorization implements AuthorizationManager<RequestAuthorizationContext> {
    private final AdministratorRepository admins;
    private final UnitBindingRepository bindings;
    private final ComplaintRepository complaints;
    private final AttachmentRepository attachments;
    public ApiAuthorization(AdministratorRepository admins, UnitBindingRepository bindings,
                            ComplaintRepository complaints, AttachmentRepository attachments) {
        this.admins = admins; this.bindings = bindings; this.complaints = complaints; this.attachments = attachments;
    }
    @Override
    @Transactional(readOnly = true)
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        var auth = authentication.get();
        boolean allowed = false;
        if (auth instanceof JwtAuthenticationToken token) {
            try {
                Long user = Long.valueOf(token.getToken().getSubject());
                Long condo = ((Number) token.getToken().getClaim("condoId")).longValue();
                String role = token.getToken().getClaimAsString("role");
                var request = context.getRequest();
                String path = request.getServletPath();
                if ("RESIDENT".equals(role) && "POST".equals(request.getMethod()) && path.equals("/api/v1/complaints")) {
                    allowed = bindings.existsByResidentIdAndUnitCondominiumIdAndStatus(user, condo, BindingStatus.APPROVED);
                } else if ("MANAGER".equals(role) && admins.existsByIdAndCondominiumId(user, condo)) {
                    String[] p = path.split("/");
                    if (path.matches("/api/v1/condominiums/[0-9]+/units")) {
                        allowed = condo.equals(Long.valueOf(p[4])) && (request.getMethod().equals("POST") || request.getMethod().equals("GET"));
                    } else if (path.matches("/api/v1/bindings/[0-9]+/(approve|reject)")) {
                        allowed = request.getMethod().equals("POST") && bindings.findByIdAndUnitCondominiumId(Long.valueOf(p[4]), condo).isPresent();
                    } else if (path.equals("/api/v1/complaints") || path.equals("/api/v1/bindings")) {
                        allowed = request.getMethod().equals("GET");
                    } else if (path.matches("/api/v1/complaints/[0-9]+")) {
                        allowed = request.getMethod().equals("GET") && complaints.findByIdAndCondominiumId(Long.valueOf(p[4]), condo).isPresent();
                    } else if (path.matches("/api/v1/complaints/[0-9]+/(status|actions)")) {
                        allowed = ((p[5].equals("status") && request.getMethod().equals("PATCH")) || (p[5].equals("actions") && request.getMethod().equals("POST")))
                            && complaints.findByIdAndCondominiumId(Long.valueOf(p[4]), condo).isPresent();
                    } else if (path.matches("/api/v1/complaints/[0-9]+/images/[0-9]+")) {
                        allowed = request.getMethod().equals("GET") && attachments.findByIdAndComplaintIdAndComplaintCondominiumId(Long.valueOf(p[6]), Long.valueOf(p[4]), condo).isPresent();
                    }
                }
            } catch (IllegalArgumentException | NullPointerException ex) { allowed = false; }
        }
        return new AuthorizationDecision(allowed);
    }
}
