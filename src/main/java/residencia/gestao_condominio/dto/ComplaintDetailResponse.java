package residencia.gestao_condominio.dto;

import residencia.gestao_condominio.entities.ComplaintCategory;
import residencia.gestao_condominio.entities.ComplaintStatus;

import java.time.Instant;
import java.util.List;

public record ComplaintDetailResponse(
        Long id,
        String protocol,
        ComplaintCategory category,
        ComplaintStatus status,
        Instant createdAt,
        String fullDescription,
        String location,
        List<ImageLink> attachments,
        List<ActionResponse> internalActions
) {
    public record ImageLink(Long imageId, String url) {}
}
