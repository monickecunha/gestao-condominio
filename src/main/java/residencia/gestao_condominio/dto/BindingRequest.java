package residencia.gestao_condominio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import residencia.gestao_condominio.dto.validation.BCryptPasswordLength;

public record BindingRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 14) String cpf,
        @NotBlank @BCryptPasswordLength String password,
        @NotNull @Positive Long unitId) {}
