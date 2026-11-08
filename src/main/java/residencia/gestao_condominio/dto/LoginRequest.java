package residencia.gestao_condominio.dto;

public record LoginRequest(String cpf, String password, Long condoId) {}
