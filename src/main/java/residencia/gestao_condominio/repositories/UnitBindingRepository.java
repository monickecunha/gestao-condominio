package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.UnitBinding;

public interface UnitBindingRepository extends JpaRepository<UnitBinding, Long> {
    java.util.List<UnitBinding> findAllByResidentId(Long residentId);
    boolean existsByResidentIdAndUnitCondominiumIdAndStatus(Long residentId, Long condominiumId, residencia.gestao_condominio.entities.BindingStatus status);
    java.util.List<UnitBinding> findAllByUnitCondominiumId(Long condominiumId);
    java.util.Optional<UnitBinding> findByIdAndUnitCondominiumId(Long id, Long condominiumId);
}
