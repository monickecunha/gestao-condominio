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
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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

    @Autowired
    private UnitBindingRepository bindingRepository;

    @Override
    public void run(String... args) throws Exception {

        Condominium c1 = new Condominium(null, "Condomínio Residencial Bella Vista", "12345678000199", "51988887777", "Av. Principal, 1000");
        condominiumRepository.save(c1);

        Unit u1 = new Unit(null, c1, "Bloco A", "101");
        Unit u2 = new Unit(null, c1, "Bloco B", "202");
        unitRepository.saveAll(Arrays.asList(u1, u2));

        Administrator admin = new Administrator(null, "Carlos Silva", "q", "51977776666", "123456", c1);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        administratorRepository.save(admin);

        Resident r1 = new Resident(null, "Ana Souza", "22233344455", "51966665555", "123456");
        Resident r2 = new Resident(null, "Lucas Oliveira", "33344455566", "51955554444", "123456");
        r1.setPassword(passwordEncoder.encode(r1.getPassword()));
        r2.setPassword(passwordEncoder.encode(r2.getPassword()));
        residentRepository.saveAll(Arrays.asList(r1, r2));

        UnitBinding b1 = new UnitBinding();
        b1.setResident(r1);
        b1.setUnit(u1);
        b1.setStatus(BindingStatus.APPROVED);
        b1.setPrimaryResident(true);
        UnitBinding b2 = new UnitBinding();
        b2.setResident(r2);
        b2.setUnit(u2);
        b2.setStatus(BindingStatus.PENDING);
        bindingRepository.saveAll(Arrays.asList(b1, b2));

        Complaint comp1 = new Complaint();
        comp1.setDescription("Barulho excessivo após as 22h no bloco vizinho.");
        comp1.setCategory(ComplaintCategory.NOISE);
        comp1.setCreatedAt(Instant.parse("2026-11-02T20:00:00Z"));
        comp1.setStatus(ComplaintStatus.RECEIVED);
        comp1.setCondominium(c1);
        complaintRepository.save(comp1);
    }
}
