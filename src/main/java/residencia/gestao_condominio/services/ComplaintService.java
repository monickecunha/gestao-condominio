package residencia.gestao_condominio.services;

import org.springframework.stereotype.Service; //mateus F08
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import residencia.gestao_condominio.dto.ComplaintReceipt;
import residencia.gestao_condominio.dto.ComplaintTrackingResponse; //mateus F08
import residencia.gestao_condominio.dto.ComplaintTrackingResponse.StatusHistoryItem; //mateus F08
import residencia.gestao_condominio.dto.CreateComplaintRequest;
import residencia.gestao_condominio.entities.Complaint;
import residencia.gestao_condominio.repositories.ComplaintRepository;
import residencia.gestao_condominio.repositories.ComplaintStatusHistoryRepository; //mateus F08
import residencia.gestao_condominio.repositories.CondominiumRepository;

@Service
public class ComplaintService {
    private final ComplaintRepository complaints;
    private final CondominiumRepository condominiums;
    private final ComplaintStatusHistoryRepository history; //mateus F08

    public ComplaintService(ComplaintRepository complaints, CondominiumRepository condominiums, ComplaintStatusHistoryRepository history) { //mateus F08 
        this.complaints = complaints;
        this.condominiums = condominiums;
        this.history = history; //mateus F08
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

     @Transactional(readOnly = true) //mateus F08 
    public ComplaintTrackingResponse trackByProtocol(String protocol) {
        // 1. Busca a denúncia; se não existir, lança erro (vira 404)
        Complaint complaint = complaints.findByProtocol(protocol)
                .orElseThrow(EntityNotFoundException::new);

        // 2. Busca o histórico e converte cada item para o formato público
        var items = history.findAllByComplaintIdOrderByChangedAtAsc(complaint.getId()).stream()
                .map(h -> new StatusHistoryItem(
                        h.getPreviousStatus(),
                        h.getNewStatus(),
                        h.getChangedAt(),
                        h.getJustification()))
                .toList();

        // 3. updatedAt = última mudança de status (ou a criação, se nunca mudou)
        var updatedAt = items.isEmpty()
                ? complaint.getCreatedAt()
                : items.get(items.size() - 1).changedAt();

        // 4. Monta a resposta só com campos públicos
        return new ComplaintTrackingResponse(
                complaint.getProtocol(),
                complaint.getCategory(),
                complaint.getStatus(),
                complaint.getCreatedAt(),
                updatedAt,
                items);
    }
}
