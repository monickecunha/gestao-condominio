package residencia.gestao_condominio.dto;

import java.time.Instant;
import residencia.gestao_condominio.entities.Action;

public record ActionResponse(String actionDescription, Instant executedAt, String registeredBy) {
    public static ActionResponse from(Action action) {
        return new ActionResponse(action.getDescription(), action.getCreatedAt(), action.getAdministrator().getName());
    }
}
