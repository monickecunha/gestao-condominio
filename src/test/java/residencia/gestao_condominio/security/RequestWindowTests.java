package residencia.gestao_condominio.security;

import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestWindowTests {
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-15T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    @Test void expiresAtBoundaryAndRefusesNewOriginsAtCapacity() {
        var clock = new MutableClock();
        var window = new RequestWindow(2, 2, Duration.ofMinutes(1), clock);
        assertTrue(window.allow("a"));
        assertTrue(window.allow("a"));
        assertFalse(window.allow("a"));
        assertTrue(window.allow("b"));
        assertFalse(window.allow("c"));
        clock.now = clock.now.plusSeconds(60);
        assertTrue(window.allow("c"));
        assertTrue(window.allow("a"));
        assertTrue(window.allow("a"));
        assertFalse(window.allow("a"));
    }

    @Test void concurrentRequestsCannotExceedQuota() throws Exception {
        var window = new RequestWindow(10);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var calls = java.util.stream.IntStream.range(0, 50)
                    .<Callable<Boolean>>mapToObj(i -> () -> window.allow("same-origin")).toList();
            int accepted = 0;
            for (var result : executor.invokeAll(calls)) if (result.get()) accepted++;
            assertEquals(10, accepted);
        }
    }
}
