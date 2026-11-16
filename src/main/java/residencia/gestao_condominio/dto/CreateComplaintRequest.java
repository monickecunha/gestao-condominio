package residencia.gestao_condominio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import residencia.gestao_condominio.entities.ComplaintCategory;

public record CreateComplaintRequest(
        @NotNull ComplaintCategory category,
        @NotBlank String description,
        @Size(max = 255) String location) {}
