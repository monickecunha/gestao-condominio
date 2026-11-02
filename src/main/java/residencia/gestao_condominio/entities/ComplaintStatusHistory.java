package residencia.gestao_condominio.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_complaint_status_history")
public class ComplaintStatusHistory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(nullable = false, length = 30)
    private String previousStatus;

    @Column(nullable = false, length = 30)
    private String newStatus;

    @Column(nullable = false)
    private Instant changedAt;

    public ComplaintStatusHistory() {
    }

    public ComplaintStatusHistory(Long id, Complaint complaint, String previousStatus, String newStatus, Instant changedAt) {
        this.id = id;
        this.complaint = complaint;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public void setComplaint(Complaint complaint) {
        this.complaint = complaint;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplaintStatusHistory that = (ComplaintStatusHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
