package residencia.gestao_condominio.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import residencia.gestao_condominio.entities.ComplaintStatus;

public record UpdateComplaintStatusRequest(@NotNull ComplaintStatus newStatus, String justification) {
    @AssertTrue(message = "A public justification is required for rejection")
    public boolean isRejectionJustified() {
        return newStatus != ComplaintStatus.REJECTED || (justification != null && !justification.isBlank());
    }
}
