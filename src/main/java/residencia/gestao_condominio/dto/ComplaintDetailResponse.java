package residencia.gestao_condominio.dto;

import residencia.gestao_condominio.entities.ComplaintCategory;
import residencia.gestao_condominio.entities.ComplaintStatus;

import java.time.Instant;

public record ComplaintDetailResponse(
        Long id,
        String protocol,
        ComplaintCategory category,
        ComplaintStatus status,
        Instant createdAt,
        String fullDescription,
        String location,
        java.util.List<ActionResponse> internalActions
) {}