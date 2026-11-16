package residencia.gestao_condominio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import residencia.gestao_condominio.dto.validation.BCryptPasswordLength;

public record CondominiumRegistrationRequest(
        @NotNull @Valid CondominiumData condominium,
        @NotNull @Valid ManagerData manager) {

    public record CondominiumData(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 18) String cnpj,
            @Size(max = 20) String phone,
            @Size(max = 255) String address) {}

    public record ManagerData(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 14) String cpf,
            @Size(max = 20) String phone,
            @NotBlank @BCryptPasswordLength String password) {}
}
