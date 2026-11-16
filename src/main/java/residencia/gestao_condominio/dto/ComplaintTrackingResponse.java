package residencia.gestao_condominio.dto;

import java.time.Instant;
import java.util.List;

import residencia.gestao_condominio.entities.ComplaintCategory;
import residencia.gestao_condominio.entities.ComplaintStatus;

public record ComplaintTrackingResponse(
        String protocol,
        ComplaintCategory category,
        ComplaintStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<StatusHistoryItem> statusHistory
) {
    public record StatusHistoryItem(
            ComplaintStatus previousStatus,
            ComplaintStatus nextStatus,
            Instant changedAt,
            String publicJustification
    ) {}
}