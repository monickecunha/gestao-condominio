package residencia.gestao_condominio.dto;

import jakarta.validation.constraints.Pattern;

public record BindingListFilter(@Pattern(regexp = "PENDING", message = "Use PENDING") String status) {}
