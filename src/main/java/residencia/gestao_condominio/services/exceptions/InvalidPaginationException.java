package residencia.gestao_condominio.services.exceptions;

public class InvalidPaginationException extends RuntimeException {
    public InvalidPaginationException() {
        super("Pagination offset must not exceed 2147483647");
    }
}
