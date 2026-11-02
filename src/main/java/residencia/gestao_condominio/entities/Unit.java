package residencia.gestao_condominio.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "tb_unit")
public class Unit implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;

    @Column(length = 20)
    private String block;

    @Column(nullable = false, length = 20)
    private String number;

    public Unit() {
    }

    public Unit(Long id, Condominium condominium, String block, String number) {
        this.id = id;
        this.condominium = condominium;
        this.block = block;
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Condominium getCondominium() {
        return condominium;
    }

    public void setCondominium(Condominium condominium) {
        this.condominium = condominium;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Unit unit = (Unit) o;
        return Objects.equals(id, unit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
