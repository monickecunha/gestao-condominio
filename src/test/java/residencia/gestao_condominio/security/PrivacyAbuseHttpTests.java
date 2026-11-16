package residencia.gestao_condominio.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import residencia.gestao_condominio.entities.*;
import residencia.gestao_condominio.repositories.*;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("setup")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:k17", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.rate-limit.login=2", "app.security.rate-limit.complaints=2",
        "app.security.rate-limit.registrations=2"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(OutputCaptureExtension.class)
class PrivacyAbuseHttpTests {
    static final String CPF = "44455566677";
    static final String PASSWORD = "k17-secret-password";
    static final String PRIVATE_TEXT = "<script>alert('k17-private')</script> ' OR 1=1 --";
    @Value("${local.server.port}") int port;
    @Autowired ApiRateLimiter limiter;
    @Autowired ProtocolLookupLimiter protocolLimiter;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired CondominiumRepository condos;
    @Autowired AdministratorRepository admins;
    @Autowired ResidentRepository residents;
    @Autowired UnitRepository units;
    @Autowired UnitBindingRepository bindings;
    @Autowired ComplaintRepository complaints;
    @Autowired PasswordEncoder passwords;
    final HttpClient client = HttpClient.newHttpClient();
    String manager, resident, otherManager;
    Long condoId;

    @BeforeAll void prepare() throws Exception {
        var constraints = jdbc.queryForList("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'TB_CONDOMINIUM' AND CONSTRAINT_TYPE = 'UNIQUE' AND CONSTRAINT_NAME IN (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE WHERE TABLE_NAME = 'TB_CONDOMINIUM' AND COLUMN_NAME = 'SINGLETON')", String.class);
        for (var constraint : constraints) jdbc.execute("ALTER TABLE tb_condominium DROP CONSTRAINT " + constraint);
        var condo = condos.save(new Condominium(null, "K17", null, null, null));
        condoId = condo.getId();
        var other = condos.save(new Condominium(null, "Other", null, null, null));
        String hash = passwords.encode(PASSWORD);
        admins.save(new Administrator(null, "Manager", "11122233344", null, hash, condo));
        admins.save(new Administrator(null, "Other Manager", "99988877766", null, hash, other));
        var user = residents.save(new Resident(null, "K17 Resident", CPF, null, hash));
        var unit = units.save(new Unit(null, condo, null, "101"));
        var binding = new UnitBinding();
        binding.setResident(user); binding.setUnit(unit); binding.setStatus(BindingStatus.APPROVED);
        binding.setPrimaryResident(true); bindings.save(binding);
        manager = login("11122233344");
        resident = login(CPF);
        limiter.reset();
        otherManager = login("99988877766");
    }

    @BeforeEach void resetWindows() { limiter.reset(); protocolLimiter.reset(); }

    @Test void limitsLoginAttemptsRegardlessOfCredentialsOrForwardingHeaders() throws Exception {
        var first = send("POST", "/auth/login", null, "{\"cpf\":\"missing\",\"password\":\"wrong\"}");
        var second = send("POST", "/auth/login", null, "{\"cpf\":\"" + CPF + "\",\"password\":\"wrong\"}");
        assertEquals(401, first.statusCode()); assertEquals(401, second.statusCode());
        assertEquals(json.readTree(first.body()).get("message"), json.readTree(second.body()).get("message"));
        assertLimited(send("POST", "/auth/login", null, "{}"));
        // Login quota does not block an already authenticated resident's complaint quota.
        assertEquals(201, send("POST", "/complaints", resident, complaintBody()).statusCode());
    }

    @Test void jsonAndMultipartShareComplaintQuotaAndBlockedRequestsDoNotPersist() throws Exception {
        long before = complaints.count();
        assertEquals(201, send("POST", "/complaints", resident, complaintBody()).statusCode());
        String boundary = "k17boundary";
        String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"complaint\"\r\nContent-Type: application/json\r\n\r\n" + complaintBody() + "\r\n--" + boundary + "--\r\n";
        var request = request("/complaints", resident).header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        assertEquals(201, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        assertLimited(send("POST", "/complaints", resident, complaintBody()));
        assertLimited(client.send(request, HttpResponse.BodyHandlers.ofString()));
        assertEquals(before + 2, complaints.count());
    }

    @Test void failedAndUnauthenticatedSubmissionsAlsoConsumeQuota() throws Exception {
        long before = complaints.count();
        assertEquals(401, send("POST", "/complaints", null, "{}").statusCode());
        assertEquals(400, send("POST", "/complaints", resident, "{}").statusCode());
        assertLimited(send("POST", "/complaints", resident, complaintBody()));
        assertEquals(before, complaints.count());
    }

    @Test void registrationRoutesShareQuota() throws Exception {
        assertEquals(400, send("POST", "/condominiums", null, "{}").statusCode());
        assertEquals(400, send("POST", "/bindings", null, "{}").statusCode());
        assertLimited(send("POST", "/bindings/additional", resident, "{}"));
    }

    @Test void protocolLimitIncludesUnknownProtocolsAndIgnoresForwardedOrigins() throws Exception {
        for (int i = 0; i < 10; i++) assertEquals(404, send("GET", "/complaints/protocol/missing-" + i, null, null).statusCode());
        assertLimited(send("GET", "/complaints/protocol/another", null, null));
    }

    @Test void preservesPrivacyInDatabaseResponsesAndLogs(CapturedOutput output) throws Exception {
        var created = send("POST", "/complaints", resident, complaintBody());
        assertEquals(201, created.statusCode());
        var receipt = json.readTree(created.body());
        assertEquals(2, receipt.size());
        String protocol = receipt.get("protocol").asString();
        var stored = complaints.findByProtocol(protocol).orElseThrow();
        assertEquals(condoId, stored.getCondominium().getId());
        assertEquals(PRIVATE_TEXT, stored.getDescription());
        var publicResponse = send("GET", "/complaints/protocol/" + protocol, null, null);
        assertEquals(200, publicResponse.statusCode());
        var tracking = json.readTree(publicResponse.body());
        assertEquals(6, tracking.size());
        assertFalse(publicResponse.body().contains(PRIVATE_TEXT));
        assertFalse(publicResponse.body().contains(CPF));
        assertFalse(tracking.has("attachments")); assertFalse(tracking.has("internalActions"));
        var detail = send("GET", "/complaints/" + stored.getId(), manager, null);
        assertEquals(200, detail.statusCode());
        assertEquals(PRIVATE_TEXT, json.readTree(detail.body()).get("fullDescription").asString());
        assertFalse(detail.body().contains(CPF)); assertFalse(detail.body().contains("K17 Resident"));
        for (String path : List.of("/complaints/" + stored.getId(), "/complaints/" + stored.getId() + "/images/1")) {
            assertEquals(403, send("GET", path, otherManager, null).statusCode());
            assertEquals(403, send("GET", path, resident, null).statusCode());
        }
        assertEquals(0, json.readTree(send("GET", "/complaints", otherManager, null).body()).get("totalElements").asLong());
        var columns = jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'TB_COMPLAINT'", String.class);
        assertEquals(Set.of("ID", "CONDOMINIUM_ID", "PROTOCOL", "CATEGORY", "DESCRIPTION", "LOCATION", "STATUS", "CREATED_AT"), new HashSet<>(columns));
        String logs = output.getAll();
        for (String secret : List.of(CPF, PASSWORD, resident, protocol, PRIVATE_TEXT)) assertFalse(logs.contains(secret), "Sensitive value appeared in application logs");
    }

    @Test void malformedInputDoesNotEchoOrLogSensitiveValues(CapturedOutput output) throws Exception {
        String marker = "k17-private-malformed-value";
        var badLogin = send("POST", "/auth/login", null, "{\"cpf\":\"" + CPF + "\",\"password\":\"" + PASSWORD + "\",\"condoId\":\"" + marker + "\"}");
        assertEquals(400, badLogin.statusCode());
        assertEquals("Invalid request body", json.readTree(badLogin.body()).get("message").asString());
        var badComplaint = send("POST", "/complaints", resident, "{\"category\":\"" + marker + "\",\"description\":\"private\"}");
        assertEquals(400, badComplaint.statusCode());
        var badFilter = send("GET", "/complaints?category=" + marker, manager, null);
        assertEquals(400, badFilter.statusCode());
        for (var response : List.of(badLogin, badComplaint, badFilter)) {
            assertFalse(response.body().contains(marker));
            assertFalse(response.body().contains(PASSWORD));
            assertFalse(response.body().contains(CPF));
        }
        assertFalse(output.getAll().contains(marker));
        assertFalse(output.getAll().contains(PASSWORD));
    }

    @Test void persistenceFailuresDoNotLogIdentifyingValues(CapturedOutput output) {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                residents.save(new Resident(null, "private-duplicate-name", CPF, null, passwords.encode(PASSWORD))));
        assertFalse(output.getAll().contains(CPF));
        assertFalse(output.getAll().contains("private-duplicate-name"));
    }

    private String complaintBody() throws Exception {
        return json.writeValueAsString(Map.of("category", "NOISE", "description", PRIVATE_TEXT));
    }
    private String login(String cpf) throws Exception {
        var response = send("POST", "/auth/login", null, json.writeValueAsString(Map.of("cpf", cpf, "password", PASSWORD)));
        assertEquals(200, response.statusCode());
        return json.readTree(response.body()).get("token").asString();
    }
    private HttpRequest.Builder request(String path, String token) {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .header("X-Forwarded-For", UUID.randomUUID().toString())
                .header("Forwarded", "for=192.0.2." + (1 + new Random().nextInt(200)));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return builder;
    }
    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        return client.send(request(path, token).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void assertLimited(HttpResponse<String> response) throws Exception {
        assertEquals(429, response.statusCode());
        assertEquals("60", response.headers().firstValue("Retry-After").orElseThrow());
        assertTrue(response.headers().firstValue("Cache-Control").orElseThrow().contains("no-store"));
        var error = json.readTree(response.body());
        assertEquals(429, error.get("status").asInt());
        assertEquals("Too many requests. Try again later.", error.get("message").asString());
        assertTrue(error.get("validationErrors").isEmpty());
    }
}
