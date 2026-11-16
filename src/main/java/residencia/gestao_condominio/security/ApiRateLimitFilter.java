package residencia.gestao_condominio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import residencia.gestao_condominio.security.exceptions.SecurityErrors;

// Created only in SecurityFilterChain to avoid registering a second servlet filter.
final class ApiRateLimitFilter extends OncePerRequestFilter {
    private final ApiRateLimiter limiter;

    ApiRateLimitFilter(ApiRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!limiter.allow(request.getMethod(), request.getServletPath(), request.getRemoteAddr())) {
            response.setHeader("Retry-After", "60");
            response.setHeader("Cache-Control", "no-store");
            SecurityErrors.write(request, response, 429);
            return;
        }
        chain.doFilter(request, response);
    }
}
