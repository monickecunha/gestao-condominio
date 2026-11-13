package residencia.gestao_condominio.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateActionRequest(@NotBlank String actionDescription) {}
