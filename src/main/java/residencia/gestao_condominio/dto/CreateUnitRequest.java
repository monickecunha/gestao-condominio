package residencia.gestao_condominio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUnitRequest(
        @NotBlank @Size(max = 20) String number,
        @Size(max = 20) String block) {}
