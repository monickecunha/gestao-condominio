package residencia.gestao_condominio.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import residencia.gestao_condominio.entities.Condominium;
import residencia.gestao_condominio.entities.Unit;
import residencia.gestao_condominio.repositories.*;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("setup")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:k06", "spring.jpa.hibernate.ddl-auto=create-drop"})
class BindingRegistrationHttpTests {
    private static final String CPF = "44455566677";
    private static final String PASSWORD = "teste-morador";
    @Value("${local.server.port}") int port;
    @Autowired CondominiumRepository condos;
    @Autowired UnitRepository units;
    @Autowired UserRepository users;
    @Autowired UnitBindingRepository bindings;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newHttpClient();
    private Unit unit;

    @BeforeEach
    void createAvailableUnit() {
        var condo = condos.save(new Condominium(null, "Parque", null, null, null));
        unit = units.save(new Unit(null, condo, null, "101"));
    }

    @AfterEach
    void clearDatabase() {
        bindings.deleteAll();
        users.deleteAll();
        units.deleteAll();
        condos.deleteAll();
    }

    @Test
    void createsPendingBindingAndAllowsLoginButDeniesComplaint() throws Exception {
        var response = register(unit.getId(), PASSWORD);
        assertEquals(201, response.statusCode(), response.body());
        var result = json.readTree(response.body());
        assertEquals(3, result.size());
        assertEquals(unit.getId().longValue(), result.get("unitId").asLong());
        assertEquals("PENDING", result.get("status").asString());
        var binding = bindings.findById(result.get("bindingId").asLong()).orElseThrow();
        assertFalse(binding.isPrimaryResident());
        assertEquals("PENDING", binding.getStatus().name());

        var login = login(PASSWORD);
        assertEquals(200, login.statusCode());
        var session = json.readTree(login.body());
        assertEquals("RESIDENT", session.get("role").asString());
        assertEquals(unit.getCondominium().getId().longValue(), session.get("condoId").asLong());
        assertEquals(403, post("complaints", "{}", session.get("token").asString()).statusCode());
    }

    @Test
    void missingUnitLeavesNoAccountOrBinding() throws Exception {
        var response = register(unit.getId() + 100, PASSWORD);
        assertEquals(404, response.statusCode(), response.body());
        assertEquals(404, json.readTree(response.body()).get("status").asInt());
        assertEquals(0, users.count());
        assertEquals(0, bindings.count());
    }

    @Test
    void repeatedRequestPreservesOriginalAccountAndPendingBinding() throws Exception {
        assertEquals(201, register(unit.getId(), PASSWORD).statusCode());
        assertEquals(409, register(unit.getId(), "outra-senha").statusCode());
        assertEquals(1, users.count());
        assertEquals(1, bindings.count());
        assertEquals("PENDING", bindings.findAll().getFirst().getStatus().name());
        assertEquals(401, login("outra-senha").statusCode());
        assertEquals(200, login(PASSWORD).statusCode());
    }

    @Test
    void rejectsIncompleteRequestAndOversizedPasswordBeforeWriting() throws Exception {
        var invalid = post("bindings", "{}", null);
        assertEquals(400, invalid.statusCode());
        assertEquals(4, json.readTree(invalid.body()).get("validationErrors").size());
        assertEquals(400, register(unit.getId(), "á".repeat(37)).statusCode());
        assertEquals(0, users.count());
        assertEquals(0, bindings.count());
    }

    private HttpResponse<String> register(long unitId, String password) throws Exception {
        return post("bindings", json.writeValueAsString(Map.of(
                "name", "Ana", "cpf", CPF, "password", password, "unitId", unitId)), null);
    }

    private HttpResponse<String> login(String password) throws Exception {
        return post("auth/login", json.writeValueAsString(Map.of("cpf", CPF, "password", password)), null);
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
