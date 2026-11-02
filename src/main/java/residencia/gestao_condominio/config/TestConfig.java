package residencia.gestao_condominio.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import residencia.gestao_condominio.entities.*;
import residencia.gestao_condominio.repositories.*;

import java.time.Instant;
import java.util.Arrays;

@Configuration
@Profile("test") // Garante que essa carga de dados só rode no perfil de testes
public class TestConfig implements CommandLineRunner {

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private ResidentRepository residentRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Override
    public void run(String... args) throws Exception {

        Condominium c1 = new Condominium(null, "Condomínio Residencial Bella Vista", "12345678000199", "51988887777", "Av. Principal, 1000");
        condominiumRepository.save(c1);

        Unit u1 = new Unit(null, c1, "Bloco A", "101");
        Unit u2 = new Unit(null, c1, "Bloco B", "202");
        unitRepository.saveAll(Arrays.asList(u1, u2));

        Administrator admin = new Administrator(null, "Carlos Silva", "11122233344", "51977776666", "123456", c1);
        administratorRepository.save(admin);

        Resident r1 = new Resident(null, "Ana Souza", "22233344455", "51966665555", "123456", u1, "APPROVED");
        Resident r2 = new Resident(null, "Lucas Oliveira", "33344455566", "51955554444", "123456", u2, "PENDING");
        residentRepository.saveAll(Arrays.asList(r1, r2));

        Complaint comp1 = new Complaint(null, "PROT-2026-0001", "Barulho excessivo após as 22h no bloco vizinho.", Instant.parse("2026-11-02T20:00:00Z"), "OPEN", false, r1, c1);
        complaintRepository.save(comp1);
    }
}
