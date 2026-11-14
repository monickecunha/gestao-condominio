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
import residencia.gestao_condominio.entities.*;
import residencia.gestao_condominio.repositories.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("setup")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:k15", "spring.jpa.hibernate.ddl-auto=create-drop"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComplaintFilterHttpTests {
    private static final String PASSWORD = "senha-teste";
    private static final String MANAGER_CPF = "11122233344";
    private static final String OTHER_MANAGER_CPF = "99988877766";
    private static final String RESIDENT_CPF = "44455566677";

    @Value("${local.server.port}") int port;
    @Autowired CondominiumRepository condos;
    @Autowired AdministratorRepository admins;
    @Autowired ResidentRepository residents;
    @Autowired UnitRepository units;
    @Autowired UnitBindingRepository bindings;
    @Autowired ComplaintRepository complaints;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newHttpClient();

    private Complaint noiseNovember1;
    private Complaint noiseUnderReview;
    private Complaint securityLateNovember30;
    private Complaint garbageDecember;
    private Complaint otherCondoNoise;

    @BeforeAll
    void createComplaintsWithKnownDates() {
        // Mesma técnica do SecurityHttpTests: só neste banco de teste a regra de
        // "um condomínio por banco" é desligada, para provar o isolamento com filtros.
        var singleton = jdbc.queryForList("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'TB_CONDOMINIUM' AND CONSTRAINT_TYPE = 'UNIQUE' AND CONSTRAINT_NAME IN (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE WHERE TABLE_NAME = 'TB_CONDOMINIUM' AND COLUMN_NAME = 'SINGLETON')", String.class);
        for (String constraint : singleton) jdbc.execute("ALTER TABLE tb_condominium DROP CONSTRAINT " + constraint);

        var parque = condos.save(new Condominium(null, "Parque", null, null, null));
        var outro = condos.save(new Condominium(null, "Outro", null, null, null));
        admins.save(new Administrator(null, "Maria", MANAGER_CPF, null, passwords.encode(PASSWORD), parque));
        admins.save(new Administrator(null, "Joao", OTHER_MANAGER_CPF, null, passwords.encode(PASSWORD), outro));

        var unit = units.save(new Unit(null, parque, null, "101"));
        var ana = residents.save(new Resident(null, "Ana", RESIDENT_CPF, null, passwords.encode(PASSWORD)));
        var binding = new UnitBinding();
        binding.setResident(ana);
        binding.setUnit(unit);
        binding.setStatus(BindingStatus.APPROVED);
        binding.setPrimaryResident(true);
        bindings.save(binding);

        noiseNovember1 = complaint(parque, ComplaintCategory.NOISE, "2026-11-01T12:00:00Z");
        noiseUnderReview = complaint(parque, ComplaintCategory.NOISE, "2026-11-10T12:00:00Z");
        noiseUnderReview.setStatus(ComplaintStatus.UNDER_REVIEW);
        noiseUnderReview = complaints.save(noiseUnderReview);
        // 30/11 às 23:30 em Brasília = 01/12 às 02:30 em UTC: testa o fuso do filtro de período.
        securityLateNovember30 = complaint(parque, ComplaintCategory.SECURITY, "2026-12-01T02:30:00Z");
        garbageDecember = complaint(parque, ComplaintCategory.GARBAGE_VIOLATION, "2026-12-05T12:00:00Z");
        otherCondoNoise = complaint(outro, ComplaintCategory.NOISE, "2026-11-10T12:00:00Z");
    }

    @Test
    void withoutFiltersReturnsAllOwnComplaints() throws Exception {
        var page = list("", MANAGER_CPF);
        assertEquals(4, page.get("totalElements").asLong());
        assertEquals(Set.of(noiseNovember1.getProtocol(), noiseUnderReview.getProtocol(),
                securityLateNovember30.getProtocol(), garbageDecember.getProtocol()), protocols(page));
    }

    @Test
    void eachFilterAloneNarrowsTheResult() throws Exception {
        assertEquals(Set.of(noiseNovember1.getProtocol(), noiseUnderReview.getProtocol()),
                protocols(list("?category=NOISE", MANAGER_CPF)));
        assertEquals(Set.of(noiseUnderReview.getProtocol()),
                protocols(list("?status=UNDER_REVIEW", MANAGER_CPF)));
        assertEquals(Set.of(noiseUnderReview.getProtocol(), securityLateNovember30.getProtocol(), garbageDecember.getProtocol()),
                protocols(list("?from=2026-11-10", MANAGER_CPF)));
        assertEquals(Set.of(noiseNovember1.getProtocol(), noiseUnderReview.getProtocol()),
                protocols(list("?to=2026-11-10", MANAGER_CPF)));
    }

    @Test
    void combinedFiltersKeepPaginationAndIsolation() throws Exception {
        var first = list("?category=NOISE&status=UNDER_REVIEW&from=2026-11-01&to=2026-11-30", MANAGER_CPF);
        assertEquals(Set.of(noiseUnderReview.getProtocol()), protocols(first));

        var paged = list("?category=NOISE&from=2026-11-01&to=2026-11-30&page=0&size=1", MANAGER_CPF);
        assertEquals(2, paged.get("totalElements").asLong());
        assertEquals(2, paged.get("totalPages").asInt());
        assertEquals(1, paged.get("content").size());
        assertEquals(noiseUnderReview.getProtocol(), paged.get("content").get(0).get("protocol").asString(),
                "a página continua da mais recente para a mais antiga");
        var second = list("?category=NOISE&from=2026-11-01&to=2026-11-30&page=1&size=1", MANAGER_CPF);
        assertEquals(noiseNovember1.getProtocol(), second.get("content").get(0).get("protocol").asString());

        var otherManager = list("?category=NOISE", OTHER_MANAGER_CPF);
        assertEquals(Set.of(otherCondoNoise.getProtocol()), protocols(otherManager));
    }

    @Test
    void periodUsesBrasiliaTimeAndIncludesTheWholeLastDay() throws Exception {
        assertTrue(protocols(list("?to=2026-11-30", MANAGER_CPF)).contains(securityLateNovember30.getProtocol()));
        assertEquals(Set.of(securityLateNovember30.getProtocol()),
                protocols(list("?from=2026-11-30&to=2026-11-30", MANAGER_CPF)));
        assertEquals(0, list("?from=2026-12-01&to=2026-12-01", MANAGER_CPF).get("totalElements").asLong());
    }

    @Test
    void returnsAnEmptyPageWhenNothingMatches() throws Exception {
        var empty = list("?category=OTHER", MANAGER_CPF);
        assertEquals(0, empty.get("totalElements").asLong());
        assertEquals(0, empty.get("totalPages").asInt());
        assertEquals(0, empty.get("content").size());
        assertEquals(0, list("?to=2026-10-31", MANAGER_CPF).get("totalElements").asLong());
        assertEquals(0, list("?category=SECURITY&status=UNDER_REVIEW", MANAGER_CPF).get("totalElements").asLong());
    }

    @Test
    void rejectsInvalidFiltersWithBadRequest() throws Exception {
        String token = login(MANAGER_CPF);
        assertInvalidField("?from=banana", token, "from");
        assertInvalidField("?to=2026-02-30", token, "to");
        assertInvalidField("?category=BANANA", token, "category");
        assertInvalidField("?status=FINISHED", token, "status");

        var period = get("complaints?from=2026-11-30&to=2026-11-01", token);
        assertEquals(400, period.statusCode(), period.body());
        assertTrue(period.body().contains("from must be on or before to"), period.body());
    }

    @Test
    void filtersDoNotOpenTheListToResidentsOrAnonymous() throws Exception {
        assertEquals(403, get("complaints?category=NOISE", login(RESIDENT_CPF)).statusCode());
        assertEquals(401, get("complaints?category=NOISE", null).statusCode());
    }

    private void assertInvalidField(String query, String token, String field) throws Exception {
        var response = get("complaints" + query, token);
        assertEquals(400, response.statusCode(), query + " -> " + response.body());
        var errors = json.readTree(response.body()).get("validationErrors");
        assertEquals(field, errors.get(0).get("field").asString(), response.body());
        assertEquals("Invalid value", errors.get(0).get("message").asString(), response.body());
    }

    private Complaint complaint(Condominium condo, ComplaintCategory category, String createdAt) {
        var complaint = new Complaint();
        complaint.setCondominium(condo);
        complaint.setCategory(category);
        complaint.setDescription("Denuncia de teste");
        complaint.setCreatedAt(Instant.parse(createdAt));
        return complaints.save(complaint);
    }

    private JsonNode list(String query, String cpf) throws Exception {
        var response = get("complaints" + query, login(cpf));
        assertEquals(200, response.statusCode(), query + " -> " + response.body());
        return json.readTree(response.body());
    }

    private Set<String> protocols(JsonNode page) {
        Set<String> found = new HashSet<>();
        for (var item : page.get("content")) found.add(item.get("protocol").asString());
        return found;
    }

    private String login(String cpf) throws Exception {
        var body = json.writeValueAsString(Map.of("cpf", cpf, "password", PASSWORD));
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).get("token").asString();
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/" + path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}