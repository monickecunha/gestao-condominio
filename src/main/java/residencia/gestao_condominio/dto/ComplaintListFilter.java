package residencia.gestao_condominio.dto;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;
import residencia.gestao_condominio.entities.ComplaintCategory;
import residencia.gestao_condominio.entities.ComplaintStatus;

import java.time.LocalDate;

public record ComplaintListFilter(
        ComplaintCategory category,
        ComplaintStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
    @AssertTrue(message = "from must be on or before to")
    public boolean isPeriodValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}