package residencia.gestao_condominio.controllers;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice(assignableTypes = {CondominiumController.class, UnitController.class, BindingController.class})
public class CondominiumErrors {
    record FieldError(String field, String message) {}
    record ApiError(Instant timestamp, int status, String error, String message, String path,
                    List<FieldError> validationErrors) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidFields(MethodArgumentNotValidException failure, HttpServletRequest request) {
        var errors = failure.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> invalidBody(HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request body", request, List.of());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiError> notFound(HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Resource not found", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> persistenceError(DataIntegrityViolationException failure, HttpServletRequest request, HandlerMethod handler) {
        if (failure.getCause() instanceof ConstraintViolationException violation
                && violation.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
            if (handler.getBeanType() == BindingController.class) {
                return response(HttpStatus.CONFLICT, "Registration already exists", request, List.of());
            }
            String message = handler.getBeanType() == UnitController.class
                    ? "Unit already registered in this block"
                    : "Condominium or manager already registered";
            return response(HttpStatus.CONFLICT, message, request, List.of());
        }
        throw failure;
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message,
                                               HttpServletRequest request, List<FieldError> fields) {
        var body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
