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
        properties = {"spring.datasource.url=jdbc:h2:mem:k09", "spring.jpa.hibernate.ddl-auto=create-drop"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComplaintManagementHttpTests {
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

    private final Set<String> ownProtocols = new HashSet<>();
    private Complaint ownComplaint;
    private Complaint otherComplaint;

    @BeforeAll
    void createTwoCondominiums() {
        // O MVP permite um condomínio por banco. Só neste schema de teste a regra é relaxada
        // para provar o isolamento entre dois condomínios reais (mesma técnica do SecurityHttpTests).
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

        ownComplaint = complaint(parque, "Barulho apos as 22h.", "Corredor");
        ownProtocols.add(ownComplaint.getProtocol());
        ownProtocols.add(complaint(parque, "Lixo fora do lugar.", null).getProtocol());
        ownProtocols.add(complaint(parque, "Portao aberto.", "Garagem").getProtocol());
        otherComplaint = complaint(outro, "Denuncia de outro condominio.", "Salao");
    }

    @Test
    void managerListsOwnComplaintsWithPagination() throws Exception {
        String token = login(MANAGER_CPF);

        var first = json.readTree(get("complaints?page=0&size=2", token).body());
        assertEquals(0, first.get("page").asInt());
        assertEquals(2, first.get("size").asInt());
        assertEquals(3, first.get("totalElements").asLong());
        assertEquals(2, first.get("totalPages").asInt());
        assertEquals(2, first.get("content").size());

        var second = json.readTree(get("complaints?page=1&size=2", token).body());
        assertEquals(1, second.get("content").size());

        Set<String> listed = new HashSet<>();
        for (var page : new JsonNode[]{first, second}) {
            for (var item : page.get("content")) {
                assertEquals(5, item.size(), item.toString());
                assertTrue(item.has("id") && item.has("protocol") && item.has("category")
                        && item.has("createdAt") && item.has("status"), item.toString());
                assertFalse(item.has("description"));
                assertFalse(item.has("location"));
                listed.add(item.get("protocol").asString());
            }
        }
        assertEquals(ownProtocols, listed);
        assertFalse(listed.contains(otherComplaint.getProtocol()));

        var newest = Instant.parse(first.get("content").get(0).get("createdAt").asString());
        var next = Instant.parse(first.get("content").get(1).get("createdAt").asString());
        assertFalse(newest.isBefore(next), "lista deve vir da mais recente para a mais antiga");
    }

    @Test
    void appliesDefaultAndLimitsToPageParameters() throws Exception {
        String token = login(MANAGER_CPF);
        assertEquals(20, json.readTree(get("complaints", token).body()).get("size").asInt());
        assertEquals(100, json.readTree(get("complaints?size=5000", token).body()).get("size").asInt());
        assertEquals(0, json.readTree(get("complaints?page=-3", token).body()).get("page").asInt());
    }

    @Test
    void managerSeesFullDetailsOfOwnComplaint() throws Exception {
        var response = get("complaints/" + ownComplaint.getId(), login(MANAGER_CPF));
        assertEquals(200, response.statusCode(), response.body());
        var body = json.readTree(response.body());
        assertEquals(ownComplaint.getId().longValue(), body.get("id").asLong());
        assertEquals(ownComplaint.getProtocol(), body.get("protocol").asString());
        assertEquals("Barulho apos as 22h.", body.get("fullDescription").asString());
        assertEquals("Corredor", body.get("location").asString());
        assertEquals("RECEIVED", body.get("status").asString());
    }

    @Test
    void otherCondominiumManagerSeesOnlyOwnDataEvenByDirectId() throws Exception {
        String other = login(OTHER_MANAGER_CPF);
        var list = json.readTree(get("complaints", other).body());
        assertEquals(1, list.get("totalElements").asLong());
        assertEquals(otherComplaint.getProtocol(), list.get("content").get(0).get("protocol").asString());

        var direct = get("complaints/" + ownComplaint.getId(), other);
        assertEquals(403, direct.statusCode());
        assertFalse(direct.body().contains("Barulho"));
    }

    @Test
    void residentAndAnonymousCannotListOrSeeDetails() throws Exception {
        String resident = login(RESIDENT_CPF);
        assertEquals(403, get("complaints", resident).statusCode());
        assertEquals(403, get("complaints/" + ownComplaint.getId(), resident).statusCode());
        assertEquals(401, get("complaints", null).statusCode());
        assertEquals(401, get("complaints/" + ownComplaint.getId(), null).statusCode());
    }

    private Complaint complaint(Condominium condo, String description, String location) {
        var complaint = new Complaint();
        complaint.setCondominium(condo);
        complaint.setCategory(ComplaintCategory.NOISE);
        complaint.setDescription(description);
        complaint.setLocation(location);
        return complaints.save(complaint);
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
