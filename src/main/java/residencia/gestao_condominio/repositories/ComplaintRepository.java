package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import residencia.gestao_condominio.entities.ComplaintStatus;
import residencia.gestao_condominio.entities.Complaint;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    @Modifying
    @Query("update Complaint c set c.status = :next where c.id = :id and c.condominium.id = :condoId and c.status = :previous")
    int changeStatus(Long id, Long condoId, ComplaintStatus previous, ComplaintStatus next);


    java.util.List<Complaint> findAllByCondominiumId(Long condominiumId);
    java.util.Optional<Complaint> findByIdAndCondominiumId(Long id, Long condominiumId);
    java.util.Optional<Complaint> findByProtocol(String protocol);
    org.springframework.data.domain.Page<Complaint> findAllByCondominiumId(Long condominiumId, org.springframework.data.domain.Pageable pageable);
}
