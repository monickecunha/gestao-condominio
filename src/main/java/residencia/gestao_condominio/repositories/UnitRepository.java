package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.Unit;

public interface UnitRepository extends JpaRepository<Unit, Long> {
}
