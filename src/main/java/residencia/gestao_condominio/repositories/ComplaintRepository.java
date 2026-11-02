package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.Complaint;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
}
