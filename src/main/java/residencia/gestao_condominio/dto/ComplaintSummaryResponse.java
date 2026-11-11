package residencia.gestao_condominio.dto;

import residencia.gestao_condominio.entities.ComplaintCategory;
import residencia.gestao_condominio.entities.ComplaintStatus;

import java.time.Instant;

public record ComplaintSummaryResponse(
        Long id,
        String protocol,
        ComplaintCategory category,
        Instant createdAt,
        ComplaintStatus status
) {}