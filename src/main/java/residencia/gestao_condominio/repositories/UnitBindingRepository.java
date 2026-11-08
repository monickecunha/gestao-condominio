package residencia.gestao_condominio.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import residencia.gestao_condominio.dto.PendingBindingResponse;
import residencia.gestao_condominio.entities.BindingStatus;
import residencia.gestao_condominio.entities.UnitBinding;

public interface UnitBindingRepository extends JpaRepository<UnitBinding, Long> {
    boolean existsByUnitIdAndPrimarySlotIsNotNull(Long unitId);
    java.util.Optional<UnitBinding> findFirstByUnitIdAndStatusOrderByRequestedAtAscIdAsc(Long unitId, BindingStatus status);

    @Query("""
            select new residencia.gestao_condominio.dto.PendingBindingResponse(
                b.id, b.resident.name, u.id, u.number, u.block, b.status, b.requestedAt)
            from UnitBinding b join b.unit u
            where u.condominium.id = :condoId and b.status = :status
            order by b.requestedAt, b.id
            """)
    List<PendingBindingResponse> listForManager(Long condoId, BindingStatus status);

    java.util.List<UnitBinding> findAllByResidentId(Long residentId);
    boolean existsByResidentIdAndUnitCondominiumIdAndStatus(Long residentId, Long condominiumId, residencia.gestao_condominio.entities.BindingStatus status);
    java.util.List<UnitBinding> findAllByUnitCondominiumId(Long condominiumId);
    java.util.Optional<UnitBinding> findByIdAndUnitCondominiumId(Long id, Long condominiumId);
}
