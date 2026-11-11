package residencia.gestao_condominio.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import residencia.gestao_condominio.dto.CondominiumRegistrationRequest;
import residencia.gestao_condominio.dto.CondominiumRegistrationResponse;
import residencia.gestao_condominio.entities.Administrator;
import residencia.gestao_condominio.entities.Condominium;
import residencia.gestao_condominio.repositories.AdministratorRepository;
import residencia.gestao_condominio.repositories.CondominiumRepository;

@Service
public class CondominiumRegistrationService {
    private final CondominiumRepository condominiums;
    private final AdministratorRepository administrators;
    private final PasswordEncoder passwords;

    public CondominiumRegistrationService(CondominiumRepository condominiums,
                                          AdministratorRepository administrators,
                                          PasswordEncoder passwords) {
        this.condominiums = condominiums;
        this.administrators = administrators;
        this.passwords = passwords;
    }

    @Transactional
    public CondominiumRegistrationResponse register(CondominiumRegistrationRequest request) {
        var data = request.condominium();
        var manager = request.manager();
        var condo = condominiums.save(new Condominium(null, data.name(), data.cnpj(), data.phone(), data.address()));
        var admin = administrators.save(new Administrator(null, manager.name(), manager.cpf(), manager.phone(),
                passwords.encode(manager.password()), condo));
        return new CondominiumRegistrationResponse(condo.getId(), admin.getId());
    }
}
