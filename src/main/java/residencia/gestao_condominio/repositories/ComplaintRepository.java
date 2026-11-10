package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import residencia.gestao_condominio.entities.Complaint;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    java.util.List<Complaint> findAllByCondominiumId(Long condominiumId);
    java.util.Optional<Complaint> findByIdAndCondominiumId(Long id, Long condominiumId);
    java.util.Optional<Complaint> findByProtocol(String protocol); //mateus F08 
}
