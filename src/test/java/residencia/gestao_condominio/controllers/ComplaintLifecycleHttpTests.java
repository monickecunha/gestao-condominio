package residencia.gestao_condominio.controllers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import residencia.gestao_condominio.security.ProtocolLookupLimiter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("setup")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:k13", "spring.jpa.hibernate.ddl-auto=create-drop"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComplaintLifecycleHttpTests {
    private static final String PASSWORD = "senha-teste";
    private static final String MANAGER_CPF = "11122233344";
    private static final String OTHER_MANAGER_CPF = "99988877766";
    private static final String RESIDENT_CPF = "44455566677";
    private static final String DESCRIPTION = "Barulho de furadeira apos as 22h.";

    @Value("${local.server.port}") int port;
    @Autowired CondominiumRepository condos;
    @Autowired AdministratorRepository admins;
    @Autowired ResidentRepository residents;
    @Autowired UnitRepository units;
    @Autowired UnitBindingRepository bindings;
    @Autowired PasswordEncoder passwords;
    @Autowired ProtocolLookupLimiter limiter;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newHttpClient();

    private String manager;
    private String otherManager;
    private String resident;

    @BeforeAll
    void createCondominiumsAndUsers() throws Exception {
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

        manager = login(MANAGER_CPF);
        otherManager = login(OTHER_MANAGER_CPF);
        resident = login(RESIDENT_CPF);
    }

    @BeforeEach
    void resetProtocolLimiter() {
        limiter.reset();
    }

    @Test
    void residentReportsManagerHandlesAndResidentFollowsByProtocol() throws Exception {
        String protocol = registerComplaint();
        long id = findIdInList("complaints?status=RECEIVED", protocol);

        var detail = read(send("GET", "complaints/" + id, manager, null), 200);
        assertEquals(DESCRIPTION, detail.get("fullDescription").asString());
        assertEquals(0, detail.get("internalActions").size());

        var review = read(changeStatus(id, "UNDER_REVIEW", null), 200);
        assertEquals("RECEIVED", review.get("previousStatus").asString());
        assertEquals("UNDER_REVIEW", review.get("nextStatus").asString());

        var action = read(send("POST", "complaints/" + id + "/actions", manager,
                "{\"actionDescription\":\"Vistoria no corredor\"}"), 201);
        assertEquals("Vistoria no corredor", action.get("actionDescription").asString());
        assertEquals("Maria", action.get("registeredBy").asString());

        read(changeStatus(id, "IN_PROGRESS", null), 200);
        read(changeStatus(id, "RESOLVED", "Morador do 501 foi notificado."), 200);

        detail = read(send("GET", "complaints/" + id, manager, null), 200);
        assertEquals("RESOLVED", detail.get("status").asString());
        assertEquals(1, detail.get("internalActions").size());
        findIdInList("complaints?status=RESOLVED", protocol);

        var tracking = send("GET", "complaints/protocol/" + protocol, null, null);
        var publicView = read(tracking, 200);
        assertEquals("RESOLVED", publicView.get("status").asString());
        var history = publicView.get("statusHistory");
        assertEquals(3, history.size());
        assertEquals("UNDER_REVIEW", history.get(0).get("nextStatus").asString());
        assertEquals("IN_PROGRESS", history.get(1).get("nextStatus").asString());
        assertEquals("RESOLVED", history.get(2).get("nextStatus").asString());
        assertEquals("Morador do 501 foi notificado.", history.get(2).get("publicJustification").asString());
        assertFalse(tracking.body().contains("Vistoria"), "providencias sao internas");
        assertFalse(tracking.body().contains(DESCRIPTION), "descricao e interna");
    }

    @Test
    void rejectionRequiresPublicJustification() throws Exception {
        String protocol = registerComplaint();
        long id = findIdInList("complaints", protocol);
        read(changeStatus(id, "UNDER_REVIEW", null), 200);

        assertEquals(400, changeStatus(id, "REJECTED", null).statusCode());
        assertEquals(400, changeStatus(id, "REJECTED", "   ").statusCode());

        var rejected = read(changeStatus(id, "REJECTED", "Sem evidencias no horario citado."), 200);
        assertEquals("REJECTED", rejected.get("nextStatus").asString());

        var publicView = read(send("GET", "complaints/protocol/" + protocol, null, null), 200);
        assertEquals("REJECTED", publicView.get("status").asString());
        assertEquals("Sem evidencias no horario citado.",
                publicView.get("statusHistory").get(1).get("publicJustification").asString());
    }

    @Test
    void invalidTransitionsAndBodiesDoNotChangeTheComplaint() throws Exception {
        long id = findIdInList("complaints", registerComplaint());

        assertEquals(409, changeStatus(id, "RESOLVED", null).statusCode());
        assertEquals(409, changeStatus(id, "REJECTED", "Pulando a analise").statusCode());
        assertEquals(400, send("PATCH", "complaints/" + id + "/status", manager, "{\"newStatus\":\"DONE\"}").statusCode());
        assertEquals(400, send("PATCH", "complaints/" + id + "/status", manager, "{}").statusCode());
        assertEquals(400, send("POST", "complaints/" + id + "/actions", manager, "{\"actionDescription\":\"  \"}").statusCode());
        assertEquals("RECEIVED", read(send("GET", "complaints/" + id, manager, null), 200).get("status").asString());

        read(changeStatus(id, "UNDER_REVIEW", null), 200);
        read(changeStatus(id, "IN_PROGRESS", null), 200);
        read(changeStatus(id, "RESOLVED", null), 200);
        assertEquals(409, changeStatus(id, "IN_PROGRESS", null).statusCode());
        assertEquals("RESOLVED", read(send("GET", "complaints/" + id, manager, null), 200).get("status").asString());
    }

    @Test
    void onlyTheManagerOfTheSameCondominiumCanHandleTheComplaint() throws Exception {
        String protocol = registerComplaint();
        long id = findIdInList("complaints", protocol);
        String status = "complaints/" + id + "/status";
        String actions = "complaints/" + id + "/actions";
        String review = "{\"newStatus\":\"UNDER_REVIEW\"}";
        String action = "{\"actionDescription\":\"Tentativa indevida\"}";

        assertEquals(403, send("GET", "complaints/" + id, otherManager, null).statusCode());
        assertEquals(403, send("PATCH", status, otherManager, review).statusCode());
        assertEquals(403, send("POST", actions, otherManager, action).statusCode());
        assertFalse(send("GET", "complaints?size=100", otherManager, null).body().contains(protocol));

        assertEquals(403, send("PATCH", status, resident, review).statusCode());
        assertEquals(403, send("POST", actions, resident, action).statusCode());
        assertEquals(401, send("PATCH", status, null, review).statusCode());
        assertEquals(401, send("POST", actions, null, action).statusCode());

        var detail = read(send("GET", "complaints/" + id, manager, null), 200);
        assertEquals("RECEIVED", detail.get("status").asString());
        assertEquals(0, detail.get("internalActions").size());
    }

    private String registerComplaint() throws Exception {
        var body = json.writeValueAsString(Map.of("category", "NOISE", "description", DESCRIPTION, "location", "Corredor"));
        return read(send("POST", "complaints", resident, body), 201).get("protocol").asString();
    }

    private long findIdInList(String path, String protocol) throws Exception {
        var page = read(send("GET", path + (path.contains("?") ? "&" : "?") + "size=100", manager, null), 200);
        for (var item : page.get("content")) {
            if (protocol.equals(item.get("protocol").asString())) return item.get("id").asLong();
        }
        return fail("protocolo nao encontrado em " + path);
    }

    private HttpResponse<String> changeStatus(long id, String newStatus, String justification) throws Exception {
        var body = justification == null
                ? Map.of("newStatus", newStatus)
                : Map.of("newStatus", newStatus, "justification", justification);
        return send("PATCH", "complaints/" + id + "/status", manager, json.writeValueAsString(body));
    }

    private JsonNode read(HttpResponse<String> response, int expectedStatus) throws Exception {
        assertEquals(expectedStatus, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private String login(String cpf) throws Exception {
        var body = json.writeValueAsString(Map.of("cpf", cpf, "password", PASSWORD));
        return read(send("POST", "auth/login", null, body), 200).get("token").asString();
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/" + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) request.header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}