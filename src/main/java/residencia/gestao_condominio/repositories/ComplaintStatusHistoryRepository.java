package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import residencia.gestao_condominio.entities.ComplaintStatusHistory;

public interface ComplaintStatusHistoryRepository extends JpaRepository<ComplaintStatusHistory, Long> {
    java.util.List<ComplaintStatusHistory> findAllByComplaintIdOrderByChangedAtAsc(Long complaintId); //mateus F08
}
