package residencia.gestao_condominio.services.exceptions;

public class BindingConflictException extends RuntimeException {
    public BindingConflictException(String message) {
        super(message);
    }
}
