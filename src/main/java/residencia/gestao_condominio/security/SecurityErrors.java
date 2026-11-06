package residencia.gestao_condominio.security;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;

public final class SecurityErrors {
    private SecurityErrors() {}
    public static void write(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        // The URI is escaped before inclusion in JSON; no credentials or token is returned.
        String path = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "").replace("\r", "");
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status
            + ",\"error\":\"" + HttpStatus.valueOf(status).getReasonPhrase()
            + "\",\"message\":\"" + (status == 401 ? "Invalid or missing credentials" : "Access denied")
            + "\",\"path\":\"" + path + "\",\"validationErrors\":[]}");
    }
}
