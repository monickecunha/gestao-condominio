package residencia.gestao_condominio.entities;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ComplaintCategory {
    NOISE, SECURITY, COMMON_AREA_MISUSE, GARBAGE_VIOLATION, RULES_VIOLATION, OTHER;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ComplaintCategory fromApiValue(String value) {
        return valueOf(value);
    }
}
