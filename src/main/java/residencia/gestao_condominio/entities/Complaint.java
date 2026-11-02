package residencia.gestao_condominio.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_complaint")
public class Complaint implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String protocol;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 30)
    private String status; // Ex: OPEN, IN_PROGRESS, RESOLVED, REJECTED

    @Column(nullable = false)
    private Boolean isAnonymous;

    @ManyToOne
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @ManyToOne
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;

    public Complaint() {
    }

    public Complaint(Long id, String protocol, String description, Instant createdAt, String status, Boolean isAnonymous, Resident resident, Condominium condominium) {
        this.id = id;
        this.protocol = protocol;
        this.description = description;
        this.createdAt = createdAt;
        this.status = status;
        this.isAnonymous = isAnonymous;
        this.resident = resident;
        this.condominium = condominium;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean anonymous) {
        isAnonymous = anonymous;
    }

    public Resident getResident() {
        return resident;
    }

    public void setResident(Resident resident) {
        this.resident = resident;
    }

    public Condominium getCondominium() {
        return condominium;
    }

    public void setCondominium(Condominium condominium) {
        this.condominium = condominium;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Complaint complaint = (Complaint) o;
        return Objects.equals(id, complaint.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
