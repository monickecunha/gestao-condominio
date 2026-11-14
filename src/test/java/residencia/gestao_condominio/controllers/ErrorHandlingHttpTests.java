package residencia.gestao_condominio.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import residencia.gestao_condominio.security.ProtocolLookupLimiter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Error format over a real servlet container: the /error re-dispatch that caused CT04
 * (400 reported as 401) does not exist under MockMvc. Uses the users seeded by TestConfig.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:errors", "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false"})
class ErrorHandlingHttpTests {
    private static final String MANAGER_CPF = "11122233344";
    private static final String PASSWORD = "123456";
    private static final String JSON = "application/json";

    @Value("${local.server.port}") int port;
    @Autowired ObjectMapper json;
    @Autowired ProtocolLookupLimiter limiter;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void resetLimiter() {
        limiter.reset();
    }

    @Test
    void malformedLoginBodyReturns400() throws Exception {
        var response = call("POST", "/api/v1/auth/login", JSON, null, "{");
        var body = assertError(response, 400, "/api/v1/auth/login");
        assertEquals("Malformed or missing request body", body.get("message").asString());
    }

    @Test
    void loginWithoutPasswordReturns400WithFieldErrors() throws Exception {
        var response = call("POST", "/api/v1/auth/login", JSON, null, "{\"email\":\"a@b.com\"}");
        var body = assertError(response, 400, "/api/v1/auth/login");
        var fields = new HashSet<String>();
        body.get("validationErrors").forEach(error -> fields.add(error.get("field").asString()));
        assertEquals(java.util.Set.of("cpf", "password"), fields);
    }

    @Test
    void loginWithWrongCredentialsReturns401() throws Exception {
        var response = call("POST", "/api/v1/auth/login", JSON, null,
                json.writeValueAsString(Map.of("cpf", MANAGER_CPF, "password", "wrong")));
        assertError(response, 401, "/api/v1/auth/login");
        assertEquals("Bearer", response.headers().firstValue("WWW-Authenticate").orElse(null));
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        var response = call("POST", "/api/v1/auth/login", "text/plain", null, "x");
        assertError(response, 415, "/api/v1/auth/login");
    }

    @Test
    void unknownProtocolReturns404WithGenericMessage() throws Exception {
        var response = call("GET", "/api/v1/complaints/protocol/INEXISTENTE", null, null, null);
        var body = assertError(response, 404, "/api/v1/complaints/protocol/INEXISTENTE");
        assertEquals("Resource not found", body.get("message").asString());
    }

    @Test
    void protectedRouteWithoutTokenReturns401() throws Exception {
        assertError(call("GET", "/api/v1/complaints", null, null, null), 401, "/api/v1/complaints");
    }

    // Authorization is deny-by-default, so a complaint id the manager cannot see (missing or from
    // another condominium) gets the same 403 and existence is never revealed.
    @Test
    void complaintOutsideManagersCondominiumIsDenied() throws Exception {
        var response = call("GET", "/api/v1/complaints/999999", null, managerToken(), null);
        var body = assertError(response, 403, "/api/v1/complaints/999999");
        assertEquals("Access denied", body.get("message").asString());
    }

    // Unknown routes never reach the dispatcher: the authorization manager answers first.
    @Test
    void unknownRouteIsDeniedByDefault() throws Exception {
        assertError(call("GET", "/api/v1/rota-inexistente", null, null, null), 401, "/api/v1/rota-inexistente");
        assertError(call("GET", "/api/v1/rota-inexistente", null, managerToken(), null), 403, "/api/v1/rota-inexistente");
    }

    private String managerToken() throws Exception {
        var login = call("POST", "/api/v1/auth/login", JSON, null,
                json.writeValueAsString(Map.of("cpf", MANAGER_CPF, "password", PASSWORD)));
        assertEquals(200, login.statusCode(), login.body());
        return json.readTree(login.body()).get("token").asString();
    }

    private JsonNode assertError(HttpResponse<String> response, int status, String path) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith(JSON), response.headers().toString());
        JsonNode body = json.readTree(response.body());
        assertEquals(6, body.size(), response.body());
        assertDoesNotThrow(() -> Instant.parse(body.get("timestamp").asString()));
        assertEquals(status, body.get("status").asInt());
        assertEquals(HttpStatus.valueOf(status).getReasonPhrase(), body.get("error").asString());
        assertFalse(body.get("message").asString().isBlank());
        assertEquals(path, body.get("path").asString());
        assertTrue(body.get("validationErrors").isArray());
        return body;
    }

    private HttpResponse<String> call(String method, String path, String contentType, String token, String body)
            throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (contentType != null) request.header("Content-Type", contentType);
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
