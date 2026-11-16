package residencia.gestao_condominio.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import residencia.gestao_condominio.entities.Administrator;
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
    @org.springframework.beans.factory.annotation.Autowired
    residencia.gestao_condominio.security.ApiRateLimiter apiRateLimiter;

    private static final String CPF = "44455566677";
    private static final String PASSWORD = "teste-morador";
    private static final String MANAGER_CPF = "11122233344";
    private static final String ADDITIONAL_CPF = "55566677788";
    @Value("${local.server.port}") int port;
    @Autowired CondominiumRepository condos;
    @Autowired UnitRepository units;
    @Autowired UserRepository users;
    @Autowired UnitBindingRepository bindings;
    @Autowired AdministratorRepository administrators;
    @Autowired ComplaintRepository complaints;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newHttpClient();
    private Unit unit;

    @BeforeEach
    void createAvailableUnit() {
        apiRateLimiter.reset();
        var condo = condos.save(new Condominium(null, "Parque", null, null, null));
        unit = units.save(new Unit(null, condo, null, "101"));
    }

    @AfterEach
    void clearDatabase() {
        complaints.deleteAll();
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

    @Test
    void primaryRegistersAdditionalResidentWhoRequiresManagerApproval() throws Exception {
        String manager = managerToken();
        long primaryId = bindingId(register(unit.getId(), PASSWORD));
        approve(primaryId, manager);

        var created = post("bindings/additional", additionalBody(unit.getId(), PASSWORD), token(CPF));
        long additionalId = bindingId(created);
        var result = json.readTree(created.body());
        assertEquals(unit.getId().longValue(), result.get("unitId").asLong());
        assertEquals("PENDING", result.get("status").asString());
        assertFalse(bindings.findById(additionalId).orElseThrow().isPrimaryResident());

        String additionalToken = token(ADDITIONAL_CPF);
        String complaint = "{\"category\":\"NOISE\",\"description\":\"Barulho no corredor\"}";
        assertEquals(403, post("complaints", complaint, additionalToken).statusCode());
        assertEquals(403, post("bindings/" + additionalId + "/approve", "", additionalToken).statusCode());
        approve(additionalId, manager);
        assertEquals(201, post("complaints", complaint, additionalToken).statusCode());
        assertFalse(bindings.findById(additionalId).orElseThrow().isPrimaryResident());
        assertTrue(bindings.findById(primaryId).orElseThrow().isPrimaryResident());
    }

    @Test
    void deniesOtherUnitAndCallersWhoAreNotItsApprovedPrimaryResident() throws Exception {
        String manager = managerToken();
        var otherUnit = units.save(new Unit(null, unit.getCondominium(), null, "102"));
        long primaryId = bindingId(register(unit.getId(), PASSWORD));
        String primary = token(CPF);
        String body = additionalBody(unit.getId(), PASSWORD);
        assertEquals(401, post("bindings/additional", body, null).statusCode());
        assertEquals(403, post("bindings/additional", body, manager).statusCode());
        assertEquals(403, post("bindings/additional", body, primary).statusCode());
        approve(primaryId, manager);
        assertEquals(403, post("bindings/additional", additionalBody(otherUnit.getId(), PASSWORD), primary).statusCode());
        assertEquals(400, post("bindings/additional", "{}", primary).statusCode());
        assertTrue(users.findByCpf(ADDITIONAL_CPF).isEmpty());
        assertEquals(1, bindings.count());

        long additionalId = bindingId(post("bindings/additional", body, primary));
        approve(additionalId, manager);
        var thirdResident = body.replace(ADDITIONAL_CPF, "66677788899");
        assertEquals(403, post("bindings/additional", thirdResident, token(ADDITIONAL_CPF)).statusCode());
        assertEquals(2, bindings.count());
        assertTrue(users.findByCpf("66677788899").isEmpty());
    }

    @Test
    void rejectsDuplicateAdditionalRegistrationWithoutChangingAccountOrBinding() throws Exception {
        String manager = managerToken();
        approve(bindingId(register(unit.getId(), PASSWORD)), manager);
        String primary = token(CPF);
        long additionalId = bindingId(post("bindings/additional", additionalBody(unit.getId(), PASSWORD), primary));
        assertEquals(409, post("bindings/additional", additionalBody(unit.getId(), "outra-senha"), primary).statusCode());
        assertEquals(3, users.count());
        assertEquals(2, bindings.count());
        assertEquals("PENDING", bindings.findById(additionalId).orElseThrow().getStatus().name());
        assertEquals(401, login(ADDITIONAL_CPF, "outra-senha").statusCode());
        token(ADDITIONAL_CPF);
    }

    private long bindingId(HttpResponse<String> response) throws Exception {
        assertEquals(201, response.statusCode(), response.body());
        return json.readTree(response.body()).get("bindingId").asLong();
    }

    private void approve(long bindingId, String manager) throws Exception {
        var response = post("bindings/" + bindingId + "/approve", "", manager);
        assertEquals(204, response.statusCode(), response.body());
    }

    private String managerToken() throws Exception {
        administrators.save(new Administrator(null, "Síndico", MANAGER_CPF, null,
                passwords.encode(PASSWORD), unit.getCondominium()));
        return token(MANAGER_CPF);
    }

    private String token(String cpf) throws Exception {
        var response = login(cpf, PASSWORD);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).get("token").asString();
    }

    private String additionalBody(long unitId, String password) {
        return json.writeValueAsString(Map.of(
                "name", "Bruno", "cpf", ADDITIONAL_CPF, "password", password, "unitId", unitId));
    }

    private HttpResponse<String> register(long unitId, String password) throws Exception {
        return post("bindings", json.writeValueAsString(Map.of(
                "name", "Ana", "cpf", CPF, "password", password, "unitId", unitId)), null);
    }

    private HttpResponse<String> login(String password) throws Exception {
        return login(CPF, password);
    }

    private HttpResponse<String> login(String cpf, String password) throws Exception {
        return post("auth/login", json.writeValueAsString(Map.of("cpf", cpf, "password", password)), null);
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/" + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
