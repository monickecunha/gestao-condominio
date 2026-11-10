// mateus F08
package residencia.gestao_condominio.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import residencia.gestao_condominio.services.exceptions.TooManyRequestsException;

@Component
public class ProtocolLookupLimiter {

    static final int MAX_ATTEMPTS = 10;
    static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(Instant start, int count) {}

    public void check(String clientKey) {
        Instant now = Instant.now();

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> now.isAfter(e.getValue().start().plus(WINDOW)));
        }

        Window window = windows.compute(clientKey, (key, old)
                -> old == null || now.isAfter(old.start().plus(WINDOW))
                ? new Window(now, 1)
                : new Window(old.start(), old.count() + 1));

        if (window.count() > MAX_ATTEMPTS) {
            throw new TooManyRequestsException();
        }
    }

    public void reset() { //mateus F08
        windows.clear();
    }
}
