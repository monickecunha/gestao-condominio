package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.Unit;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findAllByCondominiumIdOrderByIdAsc(Long condoId);
}
