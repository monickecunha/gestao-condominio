package residencia.gestao_condominio.services;

import java.util.List;
import residencia.gestao_condominio.services.exceptions.BindingConflictException;
import jakarta.persistence.EntityNotFoundException;
import residencia.gestao_condominio.dto.PendingBindingResponse;
import org.springframework.security.access.AccessDeniedException;
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
    public void approve(Long condoId, Long bindingId) {
        var binding = lockPendingBinding(condoId, bindingId);
        Long unitId = binding.getUnit().getId();
        if (!bindings.existsByUnitIdAndPrimarySlotIsNotNull(unitId)) {
            var first = bindings.findFirstByUnitIdAndStatusOrderByRequestedAtAscIdAsc(unitId, BindingStatus.PENDING)
                    .orElseThrow();
            if (!first.getId().equals(bindingId)) {
                throw new BindingConflictException("Decide the earliest pending request for this unit first");
            }
            binding.setPrimaryResident(true);
        }
        binding.setStatus(BindingStatus.APPROVED);
    }

    @Transactional
    public void reject(Long condoId, Long bindingId) {
        var binding = lockPendingBinding(condoId, bindingId);
        binding.setStatus(BindingStatus.REJECTED);
    }

    private UnitBinding lockPendingBinding(Long condoId, Long bindingId) {
        // Serialize decisions on the same unit before reading its queue or primary resident.
        units.lockForBindingDecision(condoId, bindingId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
        var binding = bindings.findByIdAndUnitCondominiumId(bindingId, condoId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
        if (binding.getStatus() != BindingStatus.PENDING) {
            throw new BindingConflictException("Only pending bindings can be decided");
        }
        return binding;
    }

    @Transactional
    public BindingResponse register(BindingRequest request) {
        var unit = units.lockForRegistration(request.unitId())
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
