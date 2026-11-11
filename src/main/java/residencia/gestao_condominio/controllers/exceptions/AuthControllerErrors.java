package residencia.gestao_condominio.controllers.exceptions;

import java.io.IOException;
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
    @ExceptionHandler(BadCredentialsException.class)
    public void invalidCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityErrors.write(request, response, HttpServletResponse.SC_UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void accessDenied(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityErrors.write(request, response, HttpServletResponse.SC_FORBIDDEN);
    }
}
