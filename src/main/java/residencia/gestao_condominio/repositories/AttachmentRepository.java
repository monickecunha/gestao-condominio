package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
}
