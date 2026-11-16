package residencia.gestao_condominio.dto;

import residencia.gestao_condominio.entities.BindingStatus;

public record BindingResponse(Long bindingId, Long unitId, BindingStatus status) {}
