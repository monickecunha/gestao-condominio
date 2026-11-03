package residencia.gestao_condominio.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_resident")
public class Resident extends User {
    public Resident() {}
    public Resident(Long id, String name, String cpf, String phone, String password) {
        super(id, name, cpf, phone, password);
    }
}
