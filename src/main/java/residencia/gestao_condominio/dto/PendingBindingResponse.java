package residencia.gestao_condominio.dto;

import java.time.Instant;
import residencia.gestao_condominio.entities.BindingStatus;

public record PendingBindingResponse(Long bindingId, String residentName, Long unitId,
                                     String number, String block, BindingStatus status, Instant requestedAt) {}
