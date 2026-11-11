package residencia.gestao_condominio.services.exceptions;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException() {
        super("Too many requests. Try again later.");
    }
}