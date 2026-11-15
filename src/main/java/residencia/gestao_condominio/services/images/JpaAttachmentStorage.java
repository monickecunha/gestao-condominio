package residencia.gestao_condominio.services.images;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import residencia.gestao_condominio.entities.Attachment;
import residencia.gestao_condominio.entities.Complaint;
import residencia.gestao_condominio.repositories.AttachmentRepository;

@Component
public class JpaAttachmentStorage implements AttachmentStorage {
    private final AttachmentRepository attachments;

    public JpaAttachmentStorage(AttachmentRepository attachments) {
        this.attachments = attachments;
    }

    @Override
    public void store(Complaint complaint, String mimeType, byte[] bytes) {
        var attachment = new Attachment();
        attachment.setComplaint(complaint);
        attachment.setFileName(UUID.randomUUID().toString());
        attachment.setMimeType(mimeType);
        attachment.setContent(bytes);
        attachments.save(attachment);
    }

    @Override
    public List<Long> listIds(Long condominiumId, Long complaintId) {
        return attachments.listIds(condominiumId, complaintId);
    }

    @Override
    public Optional<StoredImage> find(Long condominiumId, Long complaintId, Long imageId) {
        return attachments.findByIdAndComplaintIdAndComplaintCondominiumId(imageId, complaintId, condominiumId)
                .map(attachment -> new StoredImage(attachment.getMimeType(), attachment.getContent()));
    }
}
