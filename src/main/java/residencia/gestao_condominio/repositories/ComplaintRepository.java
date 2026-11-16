package residencia.gestao_condominio.repositories;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import residencia.gestao_condominio.entities.Complaint;
import residencia.gestao_condominio.entities.ComplaintCategory;
import residencia.gestao_condominio.entities.ComplaintStatus;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    @Modifying
    @Query("update Complaint c set c.status = :next where c.id = :id and c.condominium.id = :condoId and c.status = :previous")
    int changeStatus(Long id, Long condoId, ComplaintStatus previous, ComplaintStatus next);

    java.util.List<Complaint> findAllByCondominiumId(Long condominiumId);

    java.util.Optional<Complaint> findByIdAndCondominiumId(Long id, Long condominiumId);

    java.util.Optional<Complaint> findByProtocol(String protocol);

    org.springframework.data.domain.Page<Complaint> findAllByCondominiumId(Long condominiumId, org.springframework.data.domain.Pageable pageable);

    @Query("""
            select c from Complaint c
            where c.condominium.id = :condoId
              and (:category is null or c.category = :category)
              and (:status is null or c.status = :status)
              and (:start is null or c.createdAt >= :start)
              and (:end is null or c.createdAt < :end)
            """)
    org.springframework.data.domain.Page<Complaint> search(Long condoId, ComplaintCategory category,
            ComplaintStatus status, Instant start, Instant end,
            org.springframework.data.domain.Pageable pageable);
}
