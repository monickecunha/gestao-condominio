package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.Resident;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
}
