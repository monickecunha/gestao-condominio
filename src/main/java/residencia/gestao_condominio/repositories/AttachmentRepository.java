package residencia.gestao_condominio.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import residencia.gestao_condominio.entities.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    Optional<Attachment> findByIdAndComplaintIdAndComplaintCondominiumId(Long id, Long complaintId, Long condominiumId);

    boolean existsByIdAndComplaintIdAndComplaintCondominiumId(Long id, Long complaintId, Long condominiumId);

    @Query("""
            select a.id from Attachment a
            where a.complaint.id = :complaintId and a.complaint.condominium.id = :condoId
            order by a.id
            """)
    List<Long> listIds(Long condoId, Long complaintId);
}
