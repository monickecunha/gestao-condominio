package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.Administrator;

public interface AdministratorRepository extends JpaRepository<Administrator, Long> {
    boolean existsByIdAndCondominiumId(Long id, Long condominiumId);
}
