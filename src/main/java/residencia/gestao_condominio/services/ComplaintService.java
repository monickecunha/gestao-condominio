package residencia.gestao_condominio.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import residencia.gestao_condominio.dto.ComplaintReceipt;
import residencia.gestao_condominio.dto.CreateComplaintRequest;
import residencia.gestao_condominio.entities.Complaint;
import residencia.gestao_condominio.repositories.ComplaintRepository;
import residencia.gestao_condominio.repositories.CondominiumRepository;

@Service
public class ComplaintService {
    private final ComplaintRepository complaints;
    private final CondominiumRepository condominiums;

    public ComplaintService(ComplaintRepository complaints, CondominiumRepository condominiums) {
        this.complaints = complaints;
        this.condominiums = condominiums;
    }

    @Transactional
    public ComplaintReceipt register(Long condoId, CreateComplaintRequest request) {
        var complaint = new Complaint();
        complaint.setCondominium(condominiums.getReferenceById(condoId));
        complaint.setCategory(request.category());
        complaint.setDescription(request.description());
        complaint.setLocation(request.location());
        var saved = complaints.save(complaint);
        return new ComplaintReceipt(saved.getProtocol(), "Complaint registered successfully.");
    }
}
