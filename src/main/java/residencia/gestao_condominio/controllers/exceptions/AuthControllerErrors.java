package residencia.gestao_condominio.controllers.exceptions;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import residencia.gestao_condominio.controllers.AuthController;
import residencia.gestao_condominio.security.exceptions.SecurityErrors;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthControllerErrors {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CondominiumErrors.ApiError> invalidBody(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new CondominiumErrors.ApiError(
                Instant.now(), 400, "Bad Request", "Invalid request body", request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public void invalidCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityErrors.write(request, response, HttpServletResponse.SC_UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void accessDenied(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityErrors.write(request, response, HttpServletResponse.SC_FORBIDDEN);
    }
}
