package residencia.gestao_condominio.controllers.exceptions;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

/**
 * Fallback error handling for the whole API. The advices scoped to specific controllers
 * (AuthControllerErrors, CondominiumErrors) keep priority; this one covers everything else
 * and always answers with the {@link ErrorResponse} envelope. Internal details are logged,
 * never returned to the client.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadableBody(HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Malformed or missing request body", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidFields(MethodArgumentNotValidException failure, HttpServletRequest request) {
        var errors = failure.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return response(HttpStatus.BAD_REQUEST, "Validation failed", request, errors, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> constraintViolations(ConstraintViolationException failure, HttpServletRequest request) {
        var errors = failure.getConstraintViolations().stream()
                .map(violation -> {
                    String field = null;
                    for (Path.Node node : violation.getPropertyPath()) {
                        field = node.getName();
                    }
                    return new ErrorResponse.FieldError(field, violation.getMessage());
                })
                .toList();
        return response(HttpStatus.BAD_REQUEST, "Validation failed", request, errors, null);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> methodValidation(HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Validation failed", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException failure, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + failure.getName() + "'", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException failure,
                                                   HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Missing required parameter '" + failure.getParameterName() + "'", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> unauthenticated(HttpServletRequest request) {
        var headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return response(HttpStatus.UNAUTHORIZED, "Invalid or missing credentials", request, List.of(), headers);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied(HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    // Generic message on purpose: the client must not learn whether the resource exists elsewhere.
    @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ErrorResponse> notFound(Exception failure, HttpServletRequest request) {
        log.debug("Resource not found on {} {}: {}", request.getMethod(), request.getRequestURI(), failure.getMessage());
        return response(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException failure,
                                                   HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", request, List.of(), failure.getHeaders());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException failure, HttpServletRequest request) {
        log.warn("Data integrity violation on {} {}: {}", request.getMethod(), request.getRequestURI(),
                failure.getMostSpecificCause().getMessage());
        return response(HttpStatus.CONFLICT, "Request conflicts with existing data", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> unsupportedMediaType(HttpMediaTypeNotSupportedException failure,
                                                       HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", request, List.of(),
                failure.getHeaders());
    }

    // Remaining Spring MVC exceptions that carry their own status (406, missing headers/parts, ResponseStatusException...).
    @ExceptionHandler({ServletException.class, ResponseStatusException.class})
    ResponseEntity<ErrorResponse> springWebError(Exception failure, HttpServletRequest request) {
        if (failure instanceof org.springframework.web.ErrorResponse springError) {
            var status = HttpStatus.resolve(springError.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return response(status, status.getReasonPhrase(), request, List.of(), springError.getHeaders());
            }
        }
        return unexpected(failure, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception failure, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), failure);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message, HttpServletRequest request) {
        return response(status, message, request, List.of(), null);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message, HttpServletRequest request,
                                                   List<ErrorResponse.FieldError> fields, HttpHeaders headers) {
        var body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), fields);
        var builder = ResponseEntity.status(status);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.body(body);
    }
}
