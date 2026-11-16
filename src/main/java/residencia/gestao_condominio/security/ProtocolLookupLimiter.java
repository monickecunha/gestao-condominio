package residencia.gestao_condominio.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import residencia.gestao_condominio.services.exceptions.TooManyRequestsException;

@Component
public class ProtocolLookupLimiter {
    private final RequestWindow window = new RequestWindow(10);

    public void check(String clientKey) {
        if (!window.allow(clientKey)) throw new TooManyRequestsException();
    }

    @Scheduled(fixedDelay = 60_000)
    public void purgeExpired() {
        window.purgeExpired();
    }

    public void reset() {
        window.reset();
    }
}
