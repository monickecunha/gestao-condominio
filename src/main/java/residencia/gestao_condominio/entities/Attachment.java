package residencia.gestao_condominio.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_attachment")
@org.hibernate.annotations.Check(constraints = "size >= 0 AND size = octet_length(content)")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private Long size;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARBINARY)
    @Column(nullable = false, length = 5242880)
    private byte[] content;

    public Attachment() {}

    public Long getId() { return id; }
    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }

    @PrePersist
    @PreUpdate
    void updateSize() {
        if (content != null) size = (long) content.length;
    }
}
