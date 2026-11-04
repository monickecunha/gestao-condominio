package residencia.gestao_condominio.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@org.hibernate.annotations.Check(constraints = "primary_slot IS NULL OR (primary_slot = 'PRIMARY' AND status = 'APPROVED')")
@Table(name = "tb_unit_binding", uniqueConstraints = {@UniqueConstraint(columnNames = {"resident_id", "unit_id"}), @UniqueConstraint(columnNames = {"unit_id", "primary_slot"})})

public class UnitBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BindingStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    public UnitBinding() {}

    public Long getId() { return id; }
    public Resident getResident() { return resident; }
    public void setResident(Resident resident) { this.resident = resident; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public BindingStatus getStatus() { return status; }
    public void setStatus(BindingStatus status) { this.status = status; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    // NULL for non-primary bindings; the unique constraint permits one PRIMARY per unit.
    @Column(name = "primary_slot", length = 7)
    private String primarySlot;

    public boolean isPrimaryResident() { return "PRIMARY".equals(primarySlot); }
    public void setPrimaryResident(boolean primary) { primarySlot = primary ? "PRIMARY" : null; }

    @PrePersist
    @PreUpdate
    void validateBinding() {
        if (status == null) status = BindingStatus.PENDING;
        if (requestedAt == null) requestedAt = Instant.now();
        if (isPrimaryResident() && status != BindingStatus.APPROVED) {
            throw new IllegalStateException("Only an approved binding may be primary");
        }
    }
}
