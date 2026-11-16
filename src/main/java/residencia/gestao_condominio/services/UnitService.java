package residencia.gestao_condominio.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import residencia.gestao_condominio.dto.CreateUnitRequest;
import residencia.gestao_condominio.dto.UnitResponse;
import residencia.gestao_condominio.entities.Unit;
import residencia.gestao_condominio.repositories.CondominiumRepository;
import residencia.gestao_condominio.repositories.UnitRepository;

@Service
public class UnitService {
    private final UnitRepository units;
    private final CondominiumRepository condominiums;

    public UnitService(UnitRepository units, CondominiumRepository condominiums) {
        this.units = units;
        this.condominiums = condominiums;
    }

    @Transactional
    public UnitResponse create(Long condoId, CreateUnitRequest request) {
        var condominium = condominiums.getReferenceById(condoId);
        var unit = new Unit(null, condominium, request.block(), request.number().strip());
        return UnitResponse.from(units.save(unit));
    }

    @Transactional(readOnly = true)
    public List<UnitResponse> list(Long condoId) {
        return units.findAllByCondominiumIdOrderByIdAsc(condoId).stream()
                .map(UnitResponse::from)
                .toList();
    }
}
