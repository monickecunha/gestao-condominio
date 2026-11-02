package residencia.gestao_condominio.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import residencia.gestao_condominio.entities.Condominium;
import residencia.gestao_condominio.repositories.CondominiumRepository;

import java.util.Arrays;

@Configuration
@Profile("test") // Garante que essa carga de dados só rode no perfil de testes
public class TestConfig implements CommandLineRunner {

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Override
    public void run(String... args) throws Exception {

        Condominium condo1 = new Condominium(null, "Condomínio Residencial Flores", "12345678000199", "5199999999", "Rua das Flores, 123");

        condominiumRepository.save(condo1);
    }
}
