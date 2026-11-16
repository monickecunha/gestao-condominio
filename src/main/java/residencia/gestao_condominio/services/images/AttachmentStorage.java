package residencia.gestao_condominio.services.images;

import java.util.List;
import java.util.Optional;
import residencia.gestao_condominio.entities.Complaint;

public interface AttachmentStorage {
    void store(Complaint complaint, String mimeType, byte[] bytes);

    List<Long> listIds(Long condominiumId, Long complaintId);

    Optional<StoredImage> find(Long condominiumId, Long complaintId, Long imageId);

    record StoredImage(String mimeType, byte[] bytes) {}
}
