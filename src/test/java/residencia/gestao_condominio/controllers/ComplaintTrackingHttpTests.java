package residencia.gestao_condominio.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import residencia.gestao_condominio.entities.*;
import residencia.gestao_condominio.repositories.*;
import residencia.gestao_condominio.security.ProtocolLookupLimiter;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("setup")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:k08", "spring.jpa.hibernate.ddl-auto=create-drop"})
class ComplaintTrackingHttpTests {
    private static final String CPF = "44455566677";
    private static final String PASSWORD = "teste-morador";
    private static final String DESCRIPTION = "Barulho apos as 22h.";
    private static final String LOCATION = "Corredor";

    @Value("${local.server.port}") int port;
    @Autowired CondominiumRepository condos;
    @Autowired UnitRepository units;
    @Autowired UserRepository users;
    @Autowired ResidentRepository residents;
    @Autowired UnitBindingRepository bindings;
    @Autowired ComplaintRepository complaints;
    @Autowired ComplaintStatusHistoryRepository history;
    @Autowired PasswordEncoder passwords;
    @Autowired ProtocolLookupLimiter limiter;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newHttpClient();
    private String protocol;

    @BeforeEach
    void registerComplaintAsApprovedResident() throws Exception {
        limiter.reset();
        var condo = condos.save(new Condominium(null, "Parque", null, null, null));
        var unit = units.save(new Unit(null, condo, null, "101"));
        var ana = residents.save(new Resident(null, "Ana", CPF, null, passwords.encode(PASSWORD)));
        var binding = new UnitBinding();
        binding.setResident(ana);
        binding.setUnit(unit);
        binding.setStatus(BindingStatus.APPROVED);
        binding.setPrimaryResident(true);
        bindings.save(binding);

        var login = post("auth/login", json.writeValueAsString(Map.of("cpf", CPF, "password", PASSWORD)), null);
        assertEquals(200, login.statusCode(), login.body());
        String token = json.readTree(login.body()).get("token").asString();

        var created = post("complaints", json.writeValueAsString(Map.of(
                "category", "NOISE", "description", DESCRIPTION, "location", LOCATION)), token);
        assertEquals(201, created.statusCode(), created.body());
        protocol = json.readTree(created.body()).get("protocol").asString();
    }

    @AfterEach
    void clearDatabase() {
        history.deleteAll();
        complaints.deleteAll();
        bindings.deleteAll();
        users.deleteAll();
        units.deleteAll();
        condos.deleteAll();
    }

    @Test
    void tracksComplaintWithoutLoginAndShowsOnlyPublicFields() throws Exception {
        var response = get("complaints/protocol/" + protocol);
        assertEquals(200, response.statusCode(), response.body());

        var body = json.readTree(response.body());
        assertEquals(6, body.size(), response.body());
        assertEquals(protocol, body.get("protocol").asString());
        assertEquals("NOISE", body.get("category").asString());
        assertEquals("RECEIVED", body.get("status").asString());
        assertEquals(body.get("createdAt").asString(), body.get("updatedAt").asString());
        assertEquals(0, body.get("statusHistory").size());

        assertFalse(body.has("description"));
        assertFalse(body.has("location"));
        assertFalse(response.body().contains(DESCRIPTION));
        assertFalse(response.body().contains(LOCATION));
        assertFalse(response.body().contains(CPF));
        assertFalse(response.body().contains("Ana"));
    }

    @Test
    void returnsStatusHistoryAndUsesLastChangeAsUpdatedAt() throws Exception {
        var complaint = complaints.findByProtocol(protocol).orElseThrow();
        complaint.setStatus(ComplaintStatus.UNDER_REVIEW);
        complaints.save(complaint);
        var change = new ComplaintStatusHistory();
        change.setComplaint(complaint);
        change.setPreviousStatus(ComplaintStatus.RECEIVED);
        change.setNewStatus(ComplaintStatus.UNDER_REVIEW);
        change.setChangedAt(Instant.parse("2030-01-01T10:00:00Z"));
        history.save(change);

        var response = get("complaints/protocol/" + protocol);
        assertEquals(200, response.statusCode(), response.body());
        var body = json.readTree(response.body());
        assertEquals("UNDER_REVIEW", body.get("status").asString());
        assertEquals("2030-01-01T10:00:00Z", body.get("updatedAt").asString());

        var items = body.get("statusHistory");
        assertEquals(1, items.size());
        assertEquals("RECEIVED", items.get(0).get("previousStatus").asString());
        assertEquals("UNDER_REVIEW", items.get(0).get("nextStatus").asString());
        assertEquals("2030-01-01T10:00:00Z", items.get(0).get("changedAt").asString());
    }

    @Test
    void unknownAndMalformedProtocolsGetTheSameNotFound() throws Exception {
        var unknown = get("complaints/protocol/" + UUID.randomUUID());
        var malformed = get("complaints/protocol/nao-e-um-protocolo");
        assertEquals(404, unknown.statusCode());
        assertEquals(404, malformed.statusCode());
        assertEquals(json.readTree(unknown.body()).get("message").asString(),
                json.readTree(malformed.body()).get("message").asString());
    }

    @Test
    void blocksLookupsAfterTenAttemptsPerMinute() throws Exception {
        for (int i = 1; i <= 10; i++) {
            assertEquals(404, get("complaints/protocol/tentativa-" + i).statusCode(), "tentativa " + i);
        }
        var blocked = get("complaints/protocol/" + protocol);
        assertEquals(429, blocked.statusCode(), blocked.body());
        assertFalse(blocked.body().contains("\"category\""));
    }

    @Test
    void complaintTableHasNoLinkToTheAuthor() {
        var columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'TB_COMPLAINT'", String.class);
        assertFalse(columns.isEmpty());
        assertTrue(columns.stream().noneMatch(c -> c.contains("RESIDENT") || c.contains("USER") || c.contains("UNIT")),
                "Colunas encontradas: " + columns);
        assertEquals(1, complaints.count());
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/" + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
