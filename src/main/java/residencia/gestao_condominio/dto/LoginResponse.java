package residencia.gestao_condominio.dto;

public record LoginResponse(String token, long expiresIn, String role, Long condoId) {}
