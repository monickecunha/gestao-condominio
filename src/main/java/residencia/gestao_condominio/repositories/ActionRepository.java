package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.Action;

public interface ActionRepository extends JpaRepository<Action, Long> {
    java.util.List<Action> findAllByComplaintIdOrderByCreatedAtAscIdAsc(Long complaintId);
}
