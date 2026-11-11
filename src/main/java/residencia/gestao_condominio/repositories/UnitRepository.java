package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.Unit;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Unit u where u.id = :unitId")
    Optional<Unit> lockForRegistration(Long unitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from Unit u where u.condominium.id = :condoId
            and u.id = (select b.unit.id from UnitBinding b where b.id = :bindingId)
            """)
    Optional<Unit> lockForBindingDecision(Long condoId, Long bindingId);

    List<Unit> findAllByCondominiumIdOrderByIdAsc(Long condoId);
}
