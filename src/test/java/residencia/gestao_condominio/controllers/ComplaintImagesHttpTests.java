package residencia.gestao_condominio.controllers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import residencia.gestao_condominio.entities.Administrator;
import residencia.gestao_condominio.entities.BindingStatus;
import residencia.gestao_condominio.entities.Complaint;
import residencia.gestao_condominio.entities.ComplaintCategory;
import residencia.gestao_condominio.entities.Condominium;
import residencia.gestao_condominio.entities.Resident;
import residencia.gestao_condominio.entities.Unit;
import residencia.gestao_condominio.entities.UnitBinding;
import residencia.gestao_condominio.repositories.AdministratorRepository;
import residencia.gestao_condominio.repositories.AttachmentRepository;
import residencia.gestao_condominio.repositories.ComplaintRepository;
import residencia.gestao_condominio.repositories.CondominiumRepository;
import residencia.gestao_condominio.repositories.ResidentRepository;
import residencia.gestao_condominio.repositories.UnitBindingRepository;
import residencia.gestao_condominio.repositories.UnitRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("setup")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:k14", "spring.jpa.hibernate.ddl-auto=create-drop"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComplaintImagesHttpTests {
    private static final int MIB = 1024 * 1024;
    private static final String PASSWORD = "senha-teste";
    private static final String MANAGER_CPF = "11122233344";
    private static final String OTHER_MANAGER_CPF = "99988877766";
    private static final String RESIDENT_CPF = "44455566677";
    private static final String PENDING_CPF = "22233344455";
    private static final String IDENTIFYING_METADATA = "private-camera-serial-123";
    private static final String ORIGINAL_FILENAME = "resident-original-name.jpg";
    private static final String COMPLAINT = """
            {"category":"NOISE","description":"Barulho no corredor"}
            """;
    private static final byte[] WEBP = Base64.getDecoder()
            .decode("UklGRhwAAABXRUJQVlA4TA8AAAAvAUAAAAcQ9Y/+ByKi/wEA");

    @Value("${local.server.port}") int port;
    @Autowired CondominiumRepository condos;
    @Autowired AdministratorRepository admins;
    @Autowired ResidentRepository residents;
    @Autowired UnitRepository units;
    @Autowired UnitBindingRepository bindings;
    @Autowired ComplaintRepository complaints;
    @Autowired AttachmentRepository attachments;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newHttpClient();
    private Condominium condo;
    private String managerToken;
    private String otherManagerToken;
    private String residentToken;
    private String pendingToken;

    @BeforeAll
    void createCondominiumsAndResidents() throws Exception {
        // Relax only this isolated schema to exercise the second condominium.
        String singleton = jdbc.queryForObject("""
                SELECT t.CONSTRAINT_NAME
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS t
                JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE c
                    ON c.CONSTRAINT_SCHEMA = t.CONSTRAINT_SCHEMA AND c.CONSTRAINT_NAME = t.CONSTRAINT_NAME
                WHERE t.TABLE_NAME = 'TB_CONDOMINIUM' AND t.CONSTRAINT_TYPE = 'UNIQUE'
                    AND c.COLUMN_NAME = 'SINGLETON'
                """, String.class);
        jdbc.execute("ALTER TABLE tb_condominium DROP CONSTRAINT " + singleton);
        condo = condos.save(new Condominium(null, "Parque", null, null, null));
        var otherCondo = condos.save(new Condominium(null, "Outro", null, null, null));
        var passwordHash = passwords.encode(PASSWORD);
        admins.save(new Administrator(null, "Maria", MANAGER_CPF, null, passwordHash, condo));
        admins.save(new Administrator(null, "Joao", OTHER_MANAGER_CPF, null, passwordHash, otherCondo));
        var unit = units.save(new Unit(null, condo, null, "101"));
        addResident("Ana", RESIDENT_CPF, unit, BindingStatus.APPROVED, passwordHash);
        addResident("Bruno", PENDING_CPF, unit, BindingStatus.PENDING, passwordHash);
        managerToken = login(MANAGER_CPF);
        otherManagerToken = login(OTHER_MANAGER_CPF);
        residentToken = login(RESIDENT_CPF);
        pendingToken = login(PENDING_CPF);
    }

    @Test
    void persistsAndReturnsSanitizedJpegPngAndWebp() throws Exception {
        byte[] png = withPngText(image("png"));
        byte[] jpeg = withExif(image("jpeg"));
        var response = upload(residentToken, new ImagePart("image/png", png),
                new ImagePart("image/jpg", jpeg), new ImagePart("image/webp", withWebpXmp()));
        String protocol = createdProtocol(response);
        var complaint = complaints.findByProtocol(protocol).orElseThrow();
        var detailResponse = get("/api/v1/complaints/" + complaint.getId(), managerToken);
        assertEquals(200, detailResponse.statusCode());
        var images = json.readTree(detailResponse.body()).get("attachments");
        assertEquals(3, images.size());

        Set<String> types = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (JsonNode image : images) {
            assertEquals(2, image.size());
            long id = image.get("imageId").asLong();
            String url = image.get("url").asString();
            assertEquals("/api/v1/complaints/" + complaint.getId() + "/images/" + id, url);
            var stored = attachments.findByIdAndComplaintIdAndComplaintCondominiumId(
                    id, complaint.getId(), condo.getId()).orElseThrow();
            assertEquals(4, UUID.fromString(stored.getFileName().split("\\.", 2)[0]).version());
            assertTrue(names.add(stored.getFileName()));
            assertNotEquals(ORIGINAL_FILENAME, stored.getFileName());

            var download = get(url, managerToken);
            assertEquals(200, download.statusCode());
            String contentType = download.headers().firstValue("Content-Type").orElseThrow();
            assertEquals(stored.getMimeType(), contentType);
            assertTrue(types.add(contentType));
            assertEquals(download.body().length, stored.getSize().longValue());
            assertArrayEquals(stored.getContent(), download.body());
            assertFalse(new String(download.body(), ISO_8859_1).contains(IDENTIFYING_METADATA));
            var decoded = ImageIO.read(new ByteArrayInputStream(download.body()));
            assertNotNull(decoded);
            assertEquals(2, decoded.getWidth());
            assertEquals(2, decoded.getHeight());
        }
        assertEquals(Set.of("image/png", "image/jpeg", "image/webp"), types);

        var publicResponse = get("/api/v1/complaints/protocol/" + protocol, null);
        assertEquals(200, publicResponse.statusCode());
        var tracking = json.readTree(publicResponse.body());
        assertFalse(tracking.has("attachments"));
        assertFalse(tracking.has("fullDescription"));
    }

    @Test
    void acceptsMultipartWithoutImagesAndKeepsJsonSubmission() throws Exception {
        var multipart = upload(residentToken);
        var jsonResponse = post("/api/v1/complaints", "application/json", COMPLAINT.getBytes(UTF_8), residentToken);
        for (var response : List.of(multipart, jsonResponse)) {
            String protocol = createdProtocol(response);
            long id = complaints.findByProtocol(protocol).orElseThrow().getId();
            var detail = get("/api/v1/complaints/" + id, managerToken);
            assertEquals(200, detail.statusCode());
            assertEquals(0, json.readTree(detail.body()).get("attachments").size());
        }
    }

    @Test
    void rejectsMalformedAndMismatchedImagesWithoutSavingAnyPart() throws Exception {
        long complaintCount = complaints.count();
        long attachmentCount = attachments.count();
        var png = new ImagePart("image/png", image("png"));
        assertError(400, upload(residentToken, png, new ImagePart("image/png", new byte[]{1, 2, 3})));
        assertError(400, upload(residentToken, new ImagePart("image/jpeg", png.bytes())));
        assertError(400, upload(residentToken, new ImagePart("image/png", Arrays.copyOf(png.bytes(), 8))));
        assertError(400, upload(residentToken, new ImagePart("image/gif", png.bytes())));
        assertEquals(complaintCount, complaints.count());
        assertEquals(attachmentCount, attachments.count());
    }

    @Test
    void rejectsUnexpectedFilePartAndMissingComplaint() throws Exception {
        long complaintCount = complaints.count();
        long attachmentCount = attachments.count();
        var png = new ImagePart("image/png", image("png"));
        assertError(400, uploadParts(residentToken, COMPLAINT, "unexpected", png));
        assertError(400, uploadParts(residentToken, null, "images", png));
        assertEquals(complaintCount, complaints.count());
        assertEquals(attachmentCount, attachments.count());
    }

    @Test
    void enforcesFileCountPerImageAndWholeMultipartLimits() throws Exception {
        long complaintCount = complaints.count();
        long attachmentCount = attachments.count();
        var png = new ImagePart("image/png", image("png"));
        assertError(400, upload(residentToken, png, png, png, png));
        assertError(413, upload(residentToken, new ImagePart("image/png", new byte[5 * MIB + 1])));
        // Three files at the individual limit still exceed the request limit once encoded as multipart.
        var atFileLimit = new ImagePart("image/png", new byte[5 * MIB]);
        assertError(413, upload(residentToken, atFileLimit, atFileLimit, atFileLimit));
        assertEquals(complaintCount, complaints.count());
        assertEquals(attachmentCount, attachments.count());
    }

    @Test
    void requiresAnApprovedResidentForMultipartAndTheOwningManagerForRetrieval() throws Exception {
        var png = new ImagePart("image/png", image("png"));
        assertError(401, upload(null, png));
        assertError(403, upload(managerToken, png));
        assertError(403, upload(pendingToken, png));

        String protocol = createdProtocol(upload(residentToken, png));
        long id = complaints.findByProtocol(protocol).orElseThrow().getId();
        var detail = get("/api/v1/complaints/" + id, managerToken);
        assertEquals(200, detail.statusCode());
        var image = json.readTree(detail.body()).get("attachments").get(0);
        String url = image.get("url").asString();
        assertEquals(401, get(url, null).statusCode());
        assertEquals(403, get(url, residentToken).statusCode());
        assertEquals(403, get(url, otherManagerToken).statusCode());
        assertEquals(403, get("/api/v1/complaints/" + id, otherManagerToken).statusCode());
        assertEquals(403, get("/api/v1/complaints/" + id + "/images/" + Long.MAX_VALUE,
                managerToken).statusCode());

        var unrelated = new Complaint();
        unrelated.setCategory(ComplaintCategory.NOISE);
        unrelated.setDescription("Another complaint from the same condominium");
        unrelated.setCondominium(condo);
        long unrelatedId = complaints.save(unrelated).getId();
        assertEquals(403, get("/api/v1/complaints/" + unrelatedId + "/images/" + image.get("imageId").asLong(),
                managerToken).statusCode());
    }

    private void addResident(String name, String cpf, Unit unit, BindingStatus status, String passwordHash) {
        var resident = residents.save(new Resident(null, name, cpf, null, passwordHash));
        var binding = new UnitBinding();
        binding.setResident(resident);
        binding.setUnit(unit);
        binding.setStatus(status);
        binding.setPrimaryResident(status == BindingStatus.APPROVED);
        bindings.save(binding);
    }

    private String login(String cpf) throws Exception {
        byte[] body = json.writeValueAsBytes(Map.of("cpf", cpf, "password", PASSWORD));
        var response = post("/api/v1/auth/login", "application/json", body, null);
        assertEquals(200, response.statusCode());
        return json.readTree(response.body()).get("token").asString();
    }

    private String createdProtocol(HttpResponse<byte[]> response) throws Exception {
        assertEquals(201, response.statusCode(), () -> new String(response.body(), UTF_8));
        var receipt = json.readTree(response.body());
        assertEquals(2, receipt.size());
        String protocol = receipt.get("protocol").asString();
        assertEquals(4, UUID.fromString(protocol).version());
        return protocol;
    }

    private void assertError(int status, HttpResponse<byte[]> response) throws Exception {
        assertEquals(status, response.statusCode(), () -> new String(response.body(), UTF_8));
        assertEquals(status, json.readTree(response.body()).get("status").asInt());
    }

    private HttpResponse<byte[]> upload(String token, ImagePart... images) throws Exception {
        return uploadParts(token, COMPLAINT, "images", images);
    }

    private HttpResponse<byte[]> uploadParts(String token, String complaint, String imagePartName,
                                             ImagePart... images) throws Exception {
        String boundary = UUID.randomUUID().toString();
        var body = new ByteArrayOutputStream();
        if (complaint != null) {
            body.writeBytes(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"complaint\"\r\n"
                    + "Content-Type: application/json\r\n\r\n" + complaint + "\r\n").getBytes(UTF_8));
        }
        for (var image : images) {
            body.writeBytes(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + imagePartName + "\"; filename=\""
                    + ORIGINAL_FILENAME + "\"\r\n"
                    + "Content-Type: " + image.contentType() + "\r\n\r\n").getBytes(UTF_8));
            body.writeBytes(image.bytes());
            body.writeBytes("\r\n".getBytes(UTF_8));
        }
        body.writeBytes(("--" + boundary + "--\r\n").getBytes(UTF_8));
        return post("/api/v1/complaints", "multipart/form-data; boundary=" + boundary, body.toByteArray(), token);
    }

    private HttpResponse<byte[]> post(String path, String contentType, byte[] body, String token) throws Exception {
        var request = request(path, token).header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<byte[]> get(String path, String token) throws Exception {
        return client.send(request(path, token).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpRequest.Builder request(String path, String token) {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return request;
    }

    private static byte[] image(String format) throws Exception {
        var raster = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        raster.setRGB(1, 1, 0xCC3311);
        var output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(raster, format, output));
        return output.toByteArray();
    }

    private static byte[] withPngText(byte[] png) {
        byte[] type = "tEXt".getBytes(ISO_8859_1);
        byte[] data = ("Comment\0" + IDENTIFYING_METADATA).getBytes(ISO_8859_1);
        var crc = new CRC32();
        crc.update(type);
        crc.update(data);
        int endOfHeader = 33;
        return ByteBuffer.allocate(png.length + 12 + data.length)
                .put(png, 0, endOfHeader)
                .putInt(data.length).put(type).put(data).putInt((int) crc.getValue())
                .put(png, endOfHeader, png.length - endOfHeader).array();
    }

    private static byte[] withExif(byte[] jpeg) {
        byte[] camera = (IDENTIFYING_METADATA + "\0").getBytes(ISO_8859_1);
        byte[] tiff = ByteBuffer.allocate(26 + camera.length).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 'I').put((byte) 'I').putShort((short) 42).putInt(8)
                .putShort((short) 1).putShort((short) 0x010f).putShort((short) 2)
                .putInt(camera.length).putInt(26).putInt(0).put(camera).array();
        byte[] signature = "Exif\0\0".getBytes(ISO_8859_1);
        return ByteBuffer.allocate(jpeg.length + 4 + signature.length + tiff.length)
                .put(jpeg, 0, 2)
                .putShort((short) 0xffe1).putShort((short) (2 + signature.length + tiff.length))
                .put(signature).put(tiff)
                .put(jpeg, 2, jpeg.length - 2).array();
    }

    private static byte[] withWebpXmp() {
        byte[] xmp = ("""
                <x:xmpmeta xmlns:x="adobe:ns:meta/">
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description xmlns:exifEX="http://cipa.jp/exif/1.0/"
                        exifEX:BodySerialNumber="%s"/>
                  </rdf:RDF>
                </x:xmpmeta>
                """.formatted(IDENTIFYING_METADATA)).getBytes(UTF_8);
        int length = WEBP.length + 18 + 8 + xmp.length + (xmp.length & 1);
        return ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".getBytes(ISO_8859_1)).putInt(length - 8).put("WEBP".getBytes(ISO_8859_1))
                .put("VP8X".getBytes(ISO_8859_1)).putInt(10)
                .put(new byte[]{4, 0, 0, 0, 1, 0, 0, 1, 0, 0}) // XMP flag, 2x2 canvas
                .put(WEBP, 12, WEBP.length - 12)
                .put("XMP ".getBytes(ISO_8859_1)).putInt(xmp.length).put(xmp).array();
    }

    private record ImagePart(String contentType, byte[] bytes) {}
}
