package residencia.gestao_condominio.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.util.Arrays;

@Configuration
@Profile("test") // Garante que essa carga de dados só rode no perfil de testes
public class TestConfig implements CommandLineRunner {

    // Aqui você injetará os repositórios assim que criá-los na K03
    // @Autowired
    // private CondominiumRepository condominiumRepository;

    // @Autowired
    // private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {

        // Exemplo do que você fará aqui na K03 para preencher o H2 com dados fictícios:

        /*
        Condominium condo1 = new Condominium(null, "Condomínio Residencial Flores", "12345678000199", "5199999999", "Rua das Flores, 123");
        condominiumRepository.save(condo1);

        User u1 = new User(null, "Monicke Cunha", "11122233344", "51988887777", "senha123");
        userRepository.save(u1);
        */

        System.out.println("Carga inicial de dados fictícios do H2 configurada com sucesso!");
    }
}
