package residencia.gestao_condominio.controllers.exceptions;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {
    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<CondominiumErrors.ApiError> error(HttpServletRequest request) {
        var code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        var status = code instanceof Integer value ? HttpStatus.resolve(value) : null;
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        var path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        var body = new CondominiumErrors.ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                status.getReasonPhrase(), path instanceof String uri ? uri : request.getRequestURI(), List.of());
        return ResponseEntity.status(status).body(body);
    }
}
