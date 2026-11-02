package residencia.gestao_condominio.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_administrator")
public class Administrator extends User{
    @OneToOne
    @JoinColumn(name = "condominium_id", unique = true, nullable = false)
    private Condominium condominium;

    public Administrator() {
        super();
    }

    public Administrator(Long id, String name, String cpf, String phone, String password, Condominium condominium) {
        super(id, name, cpf, phone, password);
        this.condominium = condominium;
    }

    public Condominium getCondominium() {
        return condominium;
    }

    public void setCondominium(Condominium condominium) {
        this.condominium = condominium;
    }
}
