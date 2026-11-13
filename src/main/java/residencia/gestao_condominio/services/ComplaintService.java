package residencia.gestao_condominio.services;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import residencia.gestao_condominio.entities.Action;
import residencia.gestao_condominio.entities.ComplaintStatusHistory;
import residencia.gestao_condominio.repositories.ActionRepository;
import residencia.gestao_condominio.repositories.AdministratorRepository;
import residencia.gestao_condominio.services.exceptions.ComplaintStatusConflictException;
import jakarta.persistence.EntityNotFoundException;
import residencia.gestao_condominio.dto.ComplaintDetailResponse;
import residencia.gestao_condominio.dto.ComplaintReceipt;
import residencia.gestao_condominio.dto.ActionResponse;
import residencia.gestao_condominio.dto.CreateActionRequest;
import residencia.gestao_condominio.dto.UpdateComplaintStatusRequest;
import residencia.gestao_condominio.dto.ComplaintSummaryResponse;
import residencia.gestao_condominio.dto.ComplaintTrackingResponse;
import residencia.gestao_condominio.dto.ComplaintTrackingResponse.StatusHistoryItem;
import residencia.gestao_condominio.dto.CreateComplaintRequest;
import residencia.gestao_condominio.dto.PageResponse;
import residencia.gestao_condominio.entities.Complaint;
import residencia.gestao_condominio.repositories.ComplaintRepository;
import residencia.gestao_condominio.repositories.ComplaintStatusHistoryRepository;
import residencia.gestao_condominio.repositories.CondominiumRepository;

@Service
public class ComplaintService {

    static final int MAX_PAGE_SIZE = 100;

    private final ComplaintRepository complaints;
    private final CondominiumRepository condominiums;
    private final ComplaintStatusHistoryRepository history;

    private final ActionRepository actions;
    private final AdministratorRepository administrators;

    public ComplaintService(ComplaintRepository complaints, CondominiumRepository condominiums, ComplaintStatusHistoryRepository history,
                            ActionRepository actions, AdministratorRepository administrators) {
        this.complaints = complaints;
        this.condominiums = condominiums;
        this.history = history;
        this.actions = actions;
        this.administrators = administrators;
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

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ComplaintTrackingResponse trackByProtocol(String protocol) {

        Complaint complaint = complaints.findByProtocol(protocol)
                .orElseThrow(EntityNotFoundException::new);

        var items = history.findAllByComplaintIdOrderByChangedAtAscIdAsc(complaint.getId()).stream()
                .map(h -> new StatusHistoryItem(
                        h.getPreviousStatus(),
                        h.getNewStatus(),
                        h.getChangedAt(),
                        h.getJustification()))
                .toList();

        var updatedAt = items.isEmpty()
                ? complaint.getCreatedAt()
                : items.get(items.size() - 1).changedAt();

        return new ComplaintTrackingResponse(
                complaint.getProtocol(),
                complaint.getCategory(),
                complaint.getStatus(),
                complaint.getCreatedAt(),
                updatedAt,
                items);
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintSummaryResponse> listForManager(Long condoId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        var result = complaints.findAllByCondominiumId(condoId, pageable)
                .map(c -> new ComplaintSummaryResponse(
                        c.getId(), c.getProtocol(), c.getCategory(), c.getCreatedAt(), c.getStatus()));
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ComplaintDetailResponse detailForManager(Long condoId, Long id) {
        Complaint c = complaints.findByIdAndCondominiumId(id, condoId)
                .orElseThrow(EntityNotFoundException::new);
        return new ComplaintDetailResponse(
                c.getId(), c.getProtocol(), c.getCategory(), c.getStatus(),
                c.getCreatedAt(), c.getDescription(), c.getLocation(),
                actions.findAllByComplaintIdOrderByCreatedAtAscIdAsc(id).stream().map(ActionResponse::from).toList());
    }

    @Transactional
    public StatusHistoryItem updateStatus(Long condoId, Long id, UpdateComplaintStatusRequest request) {
        var complaint = complaints.findByIdAndCondominiumId(id, condoId).orElseThrow(EntityNotFoundException::new);
        var previous = complaint.getStatus();
        var next = request.newStatus();
        if (!previous.canTransitionTo(next) || complaints.changeStatus(id, condoId, previous, next) != 1) {
            throw new ComplaintStatusConflictException();
        }

        var entry = new ComplaintStatusHistory();
        entry.setComplaint(complaint);
        entry.setPreviousStatus(previous);
        entry.setNewStatus(next);
        entry.setChangedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        entry.setJustification(request.justification());
        history.save(entry);
        return new StatusHistoryItem(previous, next, entry.getChangedAt(), entry.getJustification());
    }

    @Transactional
    public ActionResponse registerAction(Long condoId, Long id, Long administratorId, CreateActionRequest request) {
        var complaint = complaints.findByIdAndCondominiumId(id, condoId).orElseThrow(EntityNotFoundException::new);
        var administrator = administrators.getReferenceById(administratorId);
        var action = new Action(null, complaint, administrator, request.actionDescription(),
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        return ActionResponse.from(actions.save(action));
    }
}
