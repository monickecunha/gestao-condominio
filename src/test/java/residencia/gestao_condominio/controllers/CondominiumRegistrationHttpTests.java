package residencia.gestao_condominio.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import residencia.gestao_condominio.entities.Resident;
import residencia.gestao_condominio.repositories.*;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("setup")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:registration", "spring.jpa.hibernate.ddl-auto=create-drop"})
class CondominiumRegistrationHttpTests {
    private static final String PATH = "/api/v1/condominiums";
    private static final String CPF = "12345678901";
    private static final String PASSWORD = "teste-local";
    private static final String REGISTRATION = """
            {"condominium":{"name":"Residencial Parque"},
             "manager":{"name":"Maria","cpf":"12345678901","password":"teste-local"}}
            """;

    @Value("${local.server.port}") int port;
    @Autowired CondominiumRepository condominiums;
    @Autowired AdministratorRepository managers;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    private final HttpClient client = HttpClient.newHttpClient();

    @AfterEach
    void clearDatabase() {
        users.deleteAll();
        condominiums.deleteAll();
    }

    @Test
    void registersBothAndAuthenticatesManager() throws Exception {
        var response = post(PATH, REGISTRATION);
        assertEquals(201, response.statusCode(), response.body());
        var body = json.readTree(response.body());
        assertEquals(2, body.size());
        long condoId = body.get("condoId").asLong();
        var manager = managers.findById(body.get("managerId").asLong()).orElseThrow();
        assertEquals(condoId, manager.getCondominium().getId());
        assertTrue(passwords.matches(PASSWORD, manager.getPassword()));

        var login = post("/api/v1/auth/login", """
                {"cpf":"12345678901","password":"teste-local"}
                """);
        assertEquals(200, login.statusCode(), login.body());
        var session = json.readTree(login.body());
        assertEquals("MANAGER", session.get("role").asString());
        assertEquals(condoId, session.get("condoId").asLong());
        assertFalse(session.get("token").asString().isBlank());
    }

    @Test
    void rejectsInvalidFieldsAndMalformedJsonBeforeSaving() throws Exception {
        var response = post(PATH, REGISTRATION.replace("Residencial Parque", " ").replace(PASSWORD, "á".repeat(37)));
        assertEquals(400, response.statusCode(), response.body());
        var body = json.readTree(response.body());
        assertEquals(PATH, body.get("path").asString());
        var fields = body.get("validationErrors").values().stream()
                .map(error -> error.get("field").asString()).toList();
        assertTrue(fields.containsAll(List.of("condominium.name", "manager.password")));
        assertEquals(400, post(PATH, "{}").statusCode());
        assertEquals(400, post(PATH, "{").statusCode());
        assertEquals(0, condominiums.count());
        assertEquals(0, users.count());
    }

    @Test
    void rejectsSecondInstallationEvenWithDifferentData() throws Exception {
        assertEquals(201, post(PATH, REGISTRATION).statusCode());
        var response = post(PATH, REGISTRATION.replace("Residencial Parque", "Outro").replace(CPF, "98765432100"));
        assertEquals(409, response.statusCode(), response.body());
        assertEquals(409, json.readTree(response.body()).get("status").asInt());
        assertEquals(1, condominiums.count());
        assertEquals(1, managers.count());
        assertTrue(users.findByCpf(CPF).isPresent());
    }

    @Test
    void rollsBackCondominiumWhenManagerCpfIsAlreadyUsed() throws Exception {
        users.save(new Resident(null, "Morador", CPF, null, passwords.encode(PASSWORD)));
        var response = post(PATH, REGISTRATION);
        assertEquals(409, response.statusCode(), response.body());
        assertEquals(0, condominiums.count());
        assertEquals(0, managers.count());
        assertEquals(1, users.count());
    }

    @Test
    void simultaneousRegistrationsCreateOnlyOnePair() {
        var first = client.sendAsync(request(PATH, REGISTRATION), HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(request(PATH, REGISTRATION.replace(CPF, "98765432100")),
                HttpResponse.BodyHandlers.ofString());
        CompletableFuture.allOf(first, second).join();
        var statuses = List.of(first.join().statusCode(), second.join().statusCode());
        assertTrue(statuses.containsAll(List.of(201, 409)), statuses.toString());
        assertEquals(1, condominiums.count());
        assertEquals(1, managers.count());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(request(path, body), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest request(String path, String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }
}
