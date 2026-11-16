package residencia.gestao_condominio.dto;

import residencia.gestao_condominio.entities.Unit;

public record UnitResponse(Long id, String number, String block) {
    public static UnitResponse from(Unit unit) {
        return new UnitResponse(unit.getId(), unit.getNumber(), unit.getBlock());
    }
}
