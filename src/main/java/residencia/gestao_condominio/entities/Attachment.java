package residencia.gestao_condominio.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "tb_attachment")
public class Attachment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    public Attachment() {
    }

    public Attachment(Long id, Complaint complaint, String fileName, String fileUrl) {
        this.id = id;
        this.complaint = complaint;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attachment that = (Attachment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
