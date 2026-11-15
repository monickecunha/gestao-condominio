package residencia.gestao_condominio.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Bounded, process-local counters; never store credentials or complaint identifiers. */
final class RequestWindow {
    private final int limit;
    private final int capacity;
    private final Duration duration;
    private final Clock clock;
    private final Map<String, Counter> counters = new HashMap<>();

    private record Counter(Instant expiresAt, int attempts) {}

    RequestWindow(int limit) {
        this(limit, 10_000, Duration.ofMinutes(1), Clock.systemUTC());
    }

    RequestWindow(int limit, int capacity, Duration duration, Clock clock) {
        if (limit < 1 || capacity < 1 || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Rate limits, capacity and duration must be positive");
        }
        this.limit = limit;
        this.capacity = capacity;
        this.duration = duration;
        this.clock = clock;
    }

    synchronized boolean allow(String origin) {
        Instant now = clock.instant();
        Counter counter = counters.get(origin);
        if (counter != null && !now.isBefore(counter.expiresAt())) {
            counters.remove(origin);
            counter = null;
        }
        if (counter == null) {
            if (counters.size() >= capacity) {
                purgeExpired();
                // Fail closed rather than evicting active limits or growing indefinitely.
                if (counters.size() >= capacity) return false;
            }
            counters.put(origin, new Counter(now.plus(duration), 1));
            return true;
        }
        if (counter.attempts() >= limit) return false;
        counters.put(origin, new Counter(counter.expiresAt(), counter.attempts() + 1));
        return true;
    }

    synchronized void purgeExpired() {
        Instant now = clock.instant();
        counters.values().removeIf(counter -> !now.isBefore(counter.expiresAt()));
    }

    synchronized void reset() {
        counters.clear();
    }
}
