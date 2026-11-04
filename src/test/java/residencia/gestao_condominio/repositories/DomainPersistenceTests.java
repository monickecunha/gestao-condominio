package residencia.gestao_condominio.repositories;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import residencia.gestao_condominio.entities.*;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DomainPersistenceTests {
    @Autowired EntityManager em;
    @Autowired CondominiumRepository condominiums;
    @Autowired UnitRepository units;
    @Autowired ResidentRepository residents;
    @Autowired UnitBindingRepository bindings;
    @Autowired ComplaintRepository complaints;
    @Autowired AttachmentRepository attachments;
    @Autowired ComplaintStatusHistoryRepository history;
    @Autowired AdministratorRepository administrators;

    Condominium condominium() { return condominiums.findAll().getFirst(); }
    Unit unit(String number) { return units.saveAndFlush(new Unit(null, condominium(), null, number)); }
    Resident resident() {
        return residents.saveAndFlush(new Resident(null, "Morador teste", UUID.randomUUID().toString().substring(0, 14), null, "test-hash"));
    }
    Complaint complaint() {
        Complaint c = new Complaint();
        c.setDescription("Ocorrência de teste");
        c.setCategory(ComplaintCategory.NOISE);
        c.setCondominium(condominium());
        return complaints.saveAndFlush(c);
    }
    UnitBinding binding(Resident resident, Unit unit, boolean primary) {
        UnitBinding b = new UnitBinding();
        b.setResident(resident); b.setUnit(unit);
        b.setStatus(BindingStatus.APPROVED); b.setPrimaryResident(primary);
        return bindings.saveAndFlush(b);
    }

    @Test void savesAnonymousComplaintWithDefaultsAndOptionalLocation() {
        Complaint c = complaint();
        Long id = c.getId();
        em.clear();
        Complaint saved = complaints.findById(id).orElseThrow();
        assertEquals(ComplaintStatus.RECEIVED, saved.getStatus());
        assertNotNull(UUID.fromString(saved.getProtocol()));
        assertNotNull(saved.getCreatedAt());
        assertNull(saved.getLocation());
        assertEquals(ComplaintCategory.NOISE, saved.getCategory());
        assertEquals(0, ((Number) em.createNativeQuery("select count(*) from information_schema.columns where table_name = 'TB_COMPLAINT' and column_name in ('RESIDENT_ID','USER_ID','UNIT_ID','IS_ANONYMOUS')").getSingleResult()).intValue());
    }
    @Test void rejectsDuplicateProtocol() {
        Complaint first = complaint();
        Complaint duplicate = new Complaint();
        duplicate.setDescription("Outra"); duplicate.setCategory(ComplaintCategory.OTHER);
        duplicate.setCondominium(condominium()); duplicate.setProtocol(first.getProtocol());
        assertThrows(RuntimeException.class, () -> complaints.saveAndFlush(duplicate));
    }
    @Test void requiresCategory() {
        Complaint c = new Complaint(); c.setDescription("Sem categoria"); c.setCondominium(condominium());
        assertThrows(RuntimeException.class, () -> complaints.saveAndFlush(c));
    }
    @Test void requiresCondominium() {
        Complaint c = new Complaint(); c.setDescription("Sem condomínio"); c.setCategory(ComplaintCategory.OTHER);
        assertThrows(RuntimeException.class, () -> complaints.saveAndFlush(c));
    }
    @Test void rejectsInvalidInitialStatus() {
        Complaint c = new Complaint(); c.setDescription("Teste"); c.setCategory(ComplaintCategory.OTHER);
        c.setCondominium(condominium()); c.setStatus(ComplaintStatus.RESOLVED);
        assertThrows(RuntimeException.class, () -> complaints.saveAndFlush(c));
    }
    @Test void preservesAttachmentBytesAndMetadataAndScopesLookup() {
        Complaint c = complaint();
        Complaint other = complaint();
        Attachment a = new Attachment(); a.setComplaint(c); a.setFileName("image.png");
        a.setMimeType("image/png"); a.setContent(new byte[]{1, 2, 3});
        Long id = attachments.saveAndFlush(a).getId();
        em.clear();
        Attachment saved = attachments.findByIdAndComplaintIdAndComplaintCondominiumId(id, c.getId(), condominium().getId()).orElseThrow();
        assertArrayEquals(new byte[]{1,2,3}, saved.getContent());
        assertEquals(3L, saved.getSize()); assertEquals("image/png", saved.getMimeType());
        assertTrue(attachments.findByIdAndComplaintIdAndComplaintCondominiumId(id, other.getId(), condominium().getId()).isEmpty());
        assertTrue(attachments.findByIdAndComplaintIdAndComplaintCondominiumId(id, c.getId(), -1L).isEmpty());
    }
    @Test void storesInitialHistoryAndRejectionJustification() {
        Complaint c = complaint();
        ComplaintStatusHistory initial = new ComplaintStatusHistory();
        initial.setComplaint(c); initial.setNewStatus(ComplaintStatus.RECEIVED); initial.setChangedAt(Instant.now());
        history.saveAndFlush(initial);
        ComplaintStatusHistory rejected = new ComplaintStatusHistory();
        rejected.setComplaint(c); rejected.setPreviousStatus(ComplaintStatus.UNDER_REVIEW);
        rejected.setNewStatus(ComplaintStatus.REJECTED); rejected.setChangedAt(Instant.now()); rejected.setJustification("Motivo registrado");
        Long id = history.saveAndFlush(rejected).getId(); em.clear();
        assertEquals("Motivo registrado", history.findById(id).orElseThrow().getJustification());
    }
    @Test void rejectsRejectionWithoutJustification() {
        ComplaintStatusHistory h = new ComplaintStatusHistory(); h.setComplaint(complaint());
        h.setPreviousStatus(ComplaintStatus.UNDER_REVIEW); h.setNewStatus(ComplaintStatus.REJECTED);
        h.setChangedAt(Instant.now()); h.setJustification("   ");
        assertThrows(RuntimeException.class, () -> history.saveAndFlush(h));
    }
    @Test void rejectsDuplicateUnitWithoutBlock() {
        unit("900");
        assertThrows(RuntimeException.class, () -> unit("900"));
    }
    @Test void rejectsDuplicateResidentBinding() {
        Resident r = resident(); Unit u = unit("901"); binding(r, u, false);
        assertThrows(RuntimeException.class, () -> binding(r, u, false));
    }
    @Test void allowsAdditionalResidentsButOnlyOnePrimary() {
        Unit u = unit("902"); binding(resident(), u, true); binding(resident(), u, false);
        assertThrows(RuntimeException.class, () -> binding(resident(), u, true));
    }
    @Test void pendingBindingCannotBePrimary() {
        UnitBinding b = new UnitBinding(); b.setResident(resident()); b.setUnit(unit("903")); b.setPrimaryResident(true);
        assertThrows(RuntimeException.class, () -> bindings.saveAndFlush(b));
    }
    @Test void scopedQueriesDoNotReturnRecordsForAnotherContext() {
        Complaint c = complaint();
        assertTrue(complaints.findByIdAndCondominiumId(c.getId(), condominium().getId()).isPresent());
        assertTrue(complaints.findByIdAndCondominiumId(c.getId(), -1L).isEmpty());
        assertTrue(complaints.findAllByCondominiumId(-1L).isEmpty());
        assertTrue(bindings.findAllByUnitCondominiumId(-1L).isEmpty());
        assertFalse(administrators.existsByIdAndCondominiumId(administrators.findAll().getFirst().getId(), -1L));
    }
    @Test void rejectsSecondCondominium() {
        assertThrows(RuntimeException.class, () -> condominiums.saveAndFlush(
            new Condominium(null, "Outro condomínio", "99887766000199", null, null)));
    }
    @Test void rejectsSecondAdministratorForTheSameCondominium() {
        Administrator a = new Administrator(null, "Outro", "99999999999", null, "test-hash", condominium());
        assertThrows(RuntimeException.class, () -> administrators.saveAndFlush(a));
    }
}
