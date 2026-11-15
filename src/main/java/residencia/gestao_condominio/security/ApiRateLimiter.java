package residencia.gestao_condominio.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApiRateLimiter {
    private final RequestWindow login;
    private final RequestWindow complaints;
    private final RequestWindow registrations;

    public ApiRateLimiter(@Value("${app.security.rate-limit.login:30}") int loginLimit,
                          @Value("${app.security.rate-limit.complaints:10}") int complaintLimit,
                          @Value("${app.security.rate-limit.registrations:20}") int registrationLimit) {
        login = new RequestWindow(loginLimit);
        complaints = new RequestWindow(complaintLimit);
        registrations = new RequestWindow(registrationLimit);
    }

    public boolean allow(String method, String path, String origin) {
        if (!"POST".equals(method)) return true;
        return switch (path) {
            case "/api/v1/auth/login" -> login.allow(origin);
            case "/api/v1/complaints" -> complaints.allow(origin);
            case "/api/v1/condominiums", "/api/v1/bindings", "/api/v1/bindings/additional" -> registrations.allow(origin);
            default -> true;
        };
    }

    @Scheduled(fixedDelay = 60_000)
    public void purgeExpired() {
        login.purgeExpired();
        complaints.purgeExpired();
        registrations.purgeExpired();
    }

    public void reset() {
        login.reset();
        complaints.reset();
        registrations.reset();
    }
}
