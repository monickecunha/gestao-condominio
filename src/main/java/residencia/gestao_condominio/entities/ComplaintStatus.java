package residencia.gestao_condominio.entities;

public enum ComplaintStatus {
    RECEIVED, UNDER_REVIEW, IN_PROGRESS, RESOLVED, REJECTED;

    public boolean canTransitionTo(ComplaintStatus next) {
        return switch (this) {
            case RECEIVED -> next == UNDER_REVIEW;
            case UNDER_REVIEW -> next == IN_PROGRESS || next == REJECTED;
            case IN_PROGRESS -> next == RESOLVED;
            case RESOLVED, REJECTED -> false;
        };
    }
}
