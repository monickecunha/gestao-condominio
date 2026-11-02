package residencia.gestao_condominio.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_resident")
public class Resident extends User{
    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(nullable = false, length = 20)
    private String status; // Ex: PENDING, APPROVED, REJECTED

    public Resident() {
        super();
    }

    public Resident(Long id, String name, String cpf, String phone, String password, Unit unit, String status) {
        super(id, name, cpf, phone, password);
        this.unit = unit;
        this.status = status;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
