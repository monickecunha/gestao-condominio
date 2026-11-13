package residencia.gestao_condominio.services.exceptions;

public class ComplaintStatusConflictException extends RuntimeException {
    public ComplaintStatusConflictException() {
        super("Transition not allowed for the current complaint status");
    }
}
