package residencia.gestao_condominio.services;

import java.util.List;
import jakarta.persistence.EntityNotFoundException;
import residencia.gestao_condominio.dto.PendingBindingResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import residencia.gestao_condominio.dto.BindingRequest;
import residencia.gestao_condominio.dto.BindingResponse;
import residencia.gestao_condominio.entities.BindingStatus;
import residencia.gestao_condominio.entities.Resident;
import residencia.gestao_condominio.entities.UnitBinding;
import residencia.gestao_condominio.repositories.ResidentRepository;
import residencia.gestao_condominio.repositories.UnitBindingRepository;
import residencia.gestao_condominio.repositories.UnitRepository;

@Service
public class BindingService {
    private final UnitRepository units;
    private final ResidentRepository residents;
    private final UnitBindingRepository bindings;
    private final PasswordEncoder passwords;

    public BindingService(UnitRepository units, ResidentRepository residents,
                          UnitBindingRepository bindings, PasswordEncoder passwords) {
        this.units = units;
        this.residents = residents;
        this.bindings = bindings;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public List<PendingBindingResponse> listPending(Long condoId) {
        return bindings.listForManager(condoId, BindingStatus.PENDING);
    }

    @Transactional
    public BindingResponse register(BindingRequest request) {
        var unit = units.findById(request.unitId())
                .orElseThrow(() -> new EntityNotFoundException("Unit not found"));
        var resident = residents.save(new Resident(null, request.name(), request.cpf(), null,
                passwords.encode(request.password())));

        var binding = new UnitBinding();
        binding.setResident(resident);
        binding.setUnit(unit);
        binding.setStatus(BindingStatus.PENDING);
        bindings.save(binding);
        return new BindingResponse(binding.getId(), unit.getId(), binding.getStatus());
    }
}
