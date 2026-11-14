package residencia.gestao_condominio.controllers.exceptions;

import java.time.Instant;
import java.util.List;

/** Standard error envelope documented in docs/api_contract.md (section 1). */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path,
                            List<FieldError> validationErrors) {
    public record FieldError(String field, String message) {}
}
