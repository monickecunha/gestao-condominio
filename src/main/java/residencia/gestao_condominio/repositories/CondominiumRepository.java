package residencia.gestao_condominio.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import residencia.gestao_condominio.entities.Condominium;

public interface CondominiumRepository extends JpaRepository<Condominium, Long> {
}
