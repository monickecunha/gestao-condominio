package residencia.gestao_condominio.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String cpf, @NotBlank String password, Long condoId) {}
