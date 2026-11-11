package residencia.gestao_condominio.security;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import residencia.gestao_condominio.entities.*;
import residencia.gestao_condominio.repositories.*;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:k04", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@Import(SecurityHttpTests.ProbeController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityHttpTests {
    private static final String VALID_COMPLAINT = """
            {"category":"NOISE","description":"Barulho após as 22h."}
            """;
    @Value("${local.server.port}") int port;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired UnitRepository units;
    @Autowired CondominiumRepository condos;
    @Autowired AdministratorRepository admins;
    @Autowired UnitBindingRepository bindings;
    @Autowired ComplaintRepository complaints;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    final HttpClient client = HttpClient.newHttpClient();
    Long otherCondo;

    // Test-only handlers exercise the real filter chain without implementing K05–K10.
    @RestController
    static class ProbeController {
        @GetMapping("/api/v1/complaints")
        Map<String, String> list() { return Map.of("result", "ok"); }

        @RequestMapping({"/api/v1/complaints/{id}", "/api/v1/complaints/{id}/status",
            "/api/v1/complaints/protocol/{protocol}"})
        Map<String, String> probe() { return Map.of("result", "ok"); }
    }
    @BeforeAll void fixture() {
        // The production MVP allows one condominium per database. Relax only this test schema
        // so HTTP isolation can be exercised with two real persisted condominiums.
        var uniqueConstraints = jdbc.queryForList("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'TB_CONDOMINIUM' AND CONSTRAINT_TYPE = 'UNIQUE' AND CONSTRAINT_NAME IN (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE WHERE TABLE_NAME = 'TB_CONDOMINIUM' AND COLUMN_NAME = 'SINGLETON')", String.class);
        assertEquals(1, uniqueConstraints.size());
        for (String constraint : uniqueConstraints) jdbc.execute("ALTER TABLE tb_condominium DROP CONSTRAINT " + constraint);
        var condo = condos.save(new Condominium(null, "Outro", "99887766000155", "123", "Rua B"));
        otherCondo = condo.getId();
        admins.save(new Administrator(null, "Outro admin", "99988877766", "123", passwords.encode("123456"), condo));
    }
    HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
            .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    String login(String cpf) throws Exception {
        var response = call("POST", "/auth/login", null, "{\"cpf\":\"" + cpf + "\",\"password\":\"123456\"}");
        assertEquals(200, response.statusCode(), response.body());
        String token = response.body().split("\"token\":\"")[1].split("\"")[0];
        assertEquals(7200, decoder.decode(token).getExpiresAt().getEpochSecond() - decoder.decode(token).getIssuedAt().getEpochSecond());
        return token;
    }
    @Test void loginAndInvalidCredentials() throws Exception {
        assertEquals("MANAGER", decoder.decode(login("11122233344")).getClaimAsString("role"));
        assertEquals("RESIDENT", decoder.decode(login("22233344455")).getClaimAsString("role"));
        assertEquals(401, call("POST", "/auth/login", null, "{\"cpf\":\"11122233344\",\"password\":\"wrong\"}").statusCode());
        assertEquals(401, call("POST", "/auth/login", null, "{\"cpf\":\"unknown\",\"password\":\"wrong\"}").statusCode());
        assertEquals(401, call("POST", "/auth/login", null, "{}").statusCode());
        var hash = users.findByCpf("11122233344").orElseThrow().getPassword();
        assertNotEquals("123456", hash);
        assertTrue(passwords.matches("123456", hash));
    }
    @Test void unauthenticatedAndPublicAccess() throws Exception {
        var response = call("GET", "/complaints", null, null);
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":401"));
        assertEquals(401, call("POST", "/complaints", null, "{}").statusCode());
        assertEquals(200, call("GET", "/complaints/protocol/550e8400-e29b-41d4-a716-446655440000", null, null).statusCode());
    }
    @Test void rolesAndCurrentBinding() throws Exception {
        String approved = login("22233344455");
        String pending = login("33344455566");
        assertEquals(201, call("POST", "/complaints", approved, VALID_COMPLAINT).statusCode());
        assertEquals(403, call("POST", "/complaints", pending, "{}").statusCode());
        var binding = bindings.findAllByResidentId(users.findByCpf("33344455566").orElseThrow().getId()).getFirst();
        binding.setStatus(BindingStatus.REJECTED); bindings.save(binding);
        assertEquals(403, call("POST", "/complaints", pending, "{}").statusCode());
        binding.setStatus(BindingStatus.APPROVED); bindings.save(binding);
        assertEquals(201, call("POST", "/complaints", pending, VALID_COMPLAINT).statusCode());
        binding.setStatus(BindingStatus.PENDING); bindings.save(binding);
        assertEquals(403, call("POST", "/complaints", pending, "{}").statusCode());
        assertEquals(403, call("GET", "/complaints", approved, null).statusCode());
        assertEquals(403, call("POST", "/bindings/1/approve", approved, "{}").statusCode());
        assertEquals(403, call("PATCH", "/complaints/1/status", approved, "{}").statusCode());
        assertEquals(403, call("POST", "/complaints", login("11122233344"), "{}").statusCode());
    }
    @Test void condominiumIsolation() throws Exception {
        String manager = login("11122233344");
        String other = login("99988877766");
        Long condo = ((Number) decoder.decode(manager).getClaim("condoId")).longValue();
        Long complaint = complaints.findAllByCondominiumId(condo).getFirst().getId();
        Long binding = bindings.findAllByUnitCondominiumId(condo).getFirst().getId();
        assertEquals(201, call("POST", "/condominiums/" + condo + "/units", manager, "{\"number\":\"303\"}").statusCode());
        assertEquals(403, call("POST", "/condominiums/" + otherCondo + "/units", manager, "{}").statusCode());
        assertEquals(200, call("GET", "/complaints/" + complaint, manager, null).statusCode());
        assertEquals(403, call("GET", "/complaints/" + complaint, other, null).statusCode());
        assertEquals(403, call("PATCH", "/complaints/" + complaint + "/status", other, "{}").statusCode());
        assertEquals(403, call("POST", "/bindings/" + binding + "/approve", other, "{}").statusCode());
        assertEquals(403, call("POST", "/auth/login", null, "{\"cpf\":\"22233344455\",\"password\":\"123456\",\"condoId\":" + otherCondo + "}").statusCode());
    }
    @Test void managesUnitsWithinOwnCondominium() throws Exception {
        String manager = login("99988877766");
        String path = "/condominiums/" + otherCondo + "/units";
        var empty = call("GET", path, manager, null);
        assertEquals(200, empty.statusCode());
        assertEquals(json.readTree("[]"), json.readTree(empty.body()));

        String unit = "{\"number\":\"101\"}";
        var created = call("POST", path, manager, unit);
        assertEquals(201, created.statusCode(), created.body());
        var first = json.readTree(created.body());
        assertTrue(first.get("id").asLong() > 0);
        assertEquals("101", first.get("number").asString());
        assertEquals("", first.get("block").asString());

        var duplicate = call("POST", path, manager, "{\"number\":\"101\",\"block\":\"\"}");
        assertEquals(409, duplicate.statusCode());
        assertEquals(409, json.readTree(duplicate.body()).get("status").asInt());

        var second = call("POST", path, manager, "{\"number\":\"101\",\"block\":\"B\"}");
        assertEquals(201, second.statusCode());
        var listing = call("GET", path, manager, null);
        assertEquals(200, listing.statusCode());
        var rows = json.readTree(listing.body());
        assertEquals(2, rows.size());
        assertEquals(first, rows.get(0));
        assertEquals(json.readTree(second.body()), rows.get(1));
    }

    @Test void validatesUnitFields() throws Exception {
        String manager = login("11122233344");
        String path = "/condominiums/" + decoder.decode(manager).getClaim("condoId") + "/units";
        var invalid = call("POST", path, manager, "{}");
        assertEquals(400, invalid.statusCode());
        assertEquals("number", json.readTree(invalid.body()).get("validationErrors").get(0).get("field").asString());
        assertEquals(400, call("POST", path, manager, "{\"number\":\"  \"}").statusCode());
        String tooLong = "1".repeat(21);
        assertEquals(400, call("POST", path, manager, json.writeValueAsString(Map.of("number", tooLong))).statusCode());
        assertEquals(400, call("POST", path, manager, json.writeValueAsString(Map.of("number", "101", "block", tooLong))).statusCode());
    }

    @Test void protectsBothUnitOperations() throws Exception {
        String manager = login("11122233344");
        String resident = login("22233344455");
        String ownPath = "/condominiums/" + decoder.decode(manager).getClaim("condoId") + "/units";
        String otherPath = "/condominiums/" + otherCondo + "/units";
        for (String method : java.util.List.of("GET", "POST")) {
            String body = method.equals("POST") ? "{\"number\":\"701\"}" : null;
            assertEquals(401, call(method, ownPath, null, body).statusCode());
            assertEquals(403, call(method, ownPath, resident, body).statusCode());
            assertEquals(403, call(method, otherPath, manager, body).statusCode());
        }
    }
    @Test void listsOnlyPendingBindingsInTheAuthenticatedManagersCondominium() throws Exception {
        String manager = login("99988877766");
        var empty = call("GET", "/bindings", manager, null);
        assertEquals(200, empty.statusCode(), empty.body());
        assertEquals(json.readTree("[]"), json.readTree(empty.body()));

        var unit = units.save(new Unit(null, condos.findById(otherCondo).orElseThrow(), "B", "505"));
        try {
            var ana = registerApplicant("Ana", "44455566677", unit);
            var bruno = registerApplicant("Bruno", "55566677788", unit);
            // Deliberately give the higher ID the earlier date to test chronological ordering.
            bruno.setRequestedAt(ana.getRequestedAt().minusSeconds(60));
            bindings.save(bruno);

            var response = call("GET", "/bindings?status=PENDING", manager, null);
            assertEquals(200, response.statusCode(), response.body());
            var rows = json.readTree(response.body());
            assertEquals(2, rows.size());
            var first = rows.get(0);
            assertEquals(7, first.size());
            assertEquals(bruno.getId().longValue(), first.get("bindingId").asLong());
            assertEquals("Bruno", first.get("residentName").asString());
            assertEquals(unit.getId().longValue(), first.get("unitId").asLong());
            assertEquals("505", first.get("number").asString());
            assertEquals("B", first.get("block").asString());
            assertEquals("PENDING", first.get("status").asString());
            assertEquals(bruno.getRequestedAt(), Instant.parse(first.get("requestedAt").asString()));
            assertEquals(ana.getId().longValue(), rows.get(1).get("bindingId").asLong());
            assertEquals(rows, json.readTree(call("GET", "/bindings", manager, null).body()));

            ana.setStatus(BindingStatus.APPROVED);
            bruno.setStatus(BindingStatus.REJECTED);
            bindings.saveAll(java.util.List.of(ana, bruno));
            assertEquals(json.readTree("[]"), json.readTree(call("GET", "/bindings", manager, null).body()));
        } finally {
            bindings.deleteAll(bindings.findAllByUnitCondominiumId(otherCondo));
            units.delete(unit);
            for (String cpf : java.util.List.of("44455566677", "55566677788")) {
                users.findByCpf(cpf).ifPresent(users::delete);
            }
        }
    }

    @Test void approvesOldestApplicantAsPrimaryAndPreservesExistingPrimary() throws Exception {
        String manager = login("99988877766");
        var unit = units.save(new Unit(null, condos.findById(otherCondo).orElseThrow(), "B", "506"));
        try {
            var ana = registerApplicant("Ana", "44455566677", unit);
            var bruno = registerApplicant("Bruno", "55566677788", unit);
            String anaPath = "/bindings/" + ana.getId() + "/approve";
            String brunoPath = "/bindings/" + bruno.getId() + "/approve";
            String resident = login("44455566677");

            assertEquals(401, call("POST", anaPath, null, null).statusCode());
            assertEquals(403, call("POST", anaPath, resident, null).statusCode());
            assertEquals(403, call("POST", anaPath, login("11122233344"), null).statusCode());
            assertEquals(403, call("POST", "/bindings/999999/approve", manager, null).statusCode());
            var outOfOrder = call("POST", brunoPath, manager, null);
            assertEquals(409, outOfOrder.statusCode(), outOfOrder.body());
            assertEquals(2, json.readTree(call("GET", "/bindings", manager, null).body()).size());

            // Two concurrent decisions on the same pending request can succeed only once.
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + anaPath))
                    .header("Authorization", "Bearer " + manager).POST(HttpRequest.BodyPublishers.noBody()).build();
            var first = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var responses = java.util.List.of(first.join(), second.join());
            assertEquals(java.util.List.of(204, 409), responses.stream().map(HttpResponse::statusCode).sorted().toList());
            assertTrue(responses.stream().filter(r -> r.statusCode() == 204).allMatch(r -> r.body().isEmpty()));
            assertTrue(bindings.findById(ana.getId()).orElseThrow().isPrimaryResident());
            var remaining = json.readTree(call("GET", "/bindings", manager, null).body());
            assertEquals(1, remaining.size());
            assertEquals(bruno.getId().longValue(), remaining.get(0).get("bindingId").asLong());
            assertEquals(201, call("POST", "/complaints", resident, VALID_COMPLAINT).statusCode());

            assertEquals(204, call("POST", brunoPath, manager, null).statusCode());
            var additional = bindings.findById(bruno.getId()).orElseThrow();
            assertEquals(BindingStatus.APPROVED, additional.getStatus());
            assertFalse(additional.isPrimaryResident());
            assertTrue(bindings.findById(ana.getId()).orElseThrow().isPrimaryResident());
        } finally {
            bindings.deleteAll(bindings.findAllByUnitCondominiumId(otherCondo));
            units.delete(unit);
            for (String cpf : java.util.List.of("44455566677", "55566677788")) {
                users.findByCpf(cpf).ifPresent(users::delete);
            }
        }
    }

    @Test void rejectingFirstApplicantLeavesTheNextEligibleForPrimary() throws Exception {
        String manager = login("99988877766");
        var unit = units.save(new Unit(null, condos.findById(otherCondo).orElseThrow(), "B", "507"));
        try {
            var ana = registerApplicant("Ana", "44455566677", unit);
            var bruno = registerApplicant("Bruno", "55566677788", unit);
            String anaPath = "/bindings/" + ana.getId();
            String reject = anaPath + "/reject";
            String resident = login("44455566677");

            assertEquals(401, call("POST", reject, null, null).statusCode());
            assertEquals(403, call("POST", reject, resident, null).statusCode());
            assertEquals(403, call("POST", reject, login("11122233344"), null).statusCode());
            assertEquals(403, call("POST", "/bindings/999999/reject", manager, null).statusCode());
            var decision = call("POST", reject, manager, null);
            assertEquals(204, decision.statusCode(), decision.body());
            assertTrue(decision.body().isEmpty());
            var rejected = bindings.findById(ana.getId()).orElseThrow();
            assertEquals(BindingStatus.REJECTED, rejected.getStatus());
            assertFalse(rejected.isPrimaryResident());
            assertEquals(409, call("POST", reject, manager, null).statusCode());
            assertEquals(409, call("POST", anaPath + "/approve", manager, null).statusCode());

            var queue = json.readTree(call("GET", "/bindings", manager, null).body());
            assertEquals(1, queue.size());
            assertEquals(bruno.getId().longValue(), queue.get(0).get("bindingId").asLong());
            String brunoPath = "/bindings/" + bruno.getId();
            assertEquals(204, call("POST", brunoPath + "/approve", manager, null).statusCode());
            assertTrue(bindings.findById(bruno.getId()).orElseThrow().isPrimaryResident());
            assertEquals(409, call("POST", brunoPath + "/reject", manager, null).statusCode());
            assertEquals(403, call("POST", "/complaints", resident, "{}").statusCode());
            var credentials = Map.of("cpf", "44455566677", "password", "123456");
            var denied = call("POST", "/auth/login", null, json.writeValueAsString(credentials));
            assertEquals(403, denied.statusCode(), denied.body());
            assertFalse(json.readTree(denied.body()).has("token"));

            var scopedCredentials = Map.of("cpf", "44455566677", "password", "123456", "condoId", otherCondo);
            var scoped = call("POST", "/auth/login", null, json.writeValueAsString(scopedCredentials));
            assertEquals(403, scoped.statusCode(), scoped.body());

            var secondUnit = units.save(new Unit(null, condos.findById(otherCondo).orElseThrow(), "B", "508"));
            try {
                var approved = new UnitBinding();
                approved.setResident(ana.getResident());
                approved.setUnit(secondUnit);
                approved.setStatus(BindingStatus.APPROVED);
                bindings.save(approved);
                assertEquals(otherCondo.longValue(), decoder.decode(login("44455566677")).<Number>getClaim("condoId").longValue());
            } finally {
                bindings.deleteAll(bindings.findAllByUnitCondominiumId(otherCondo));
                units.delete(secondUnit);
            }
        } finally {
            bindings.deleteAll(bindings.findAllByUnitCondominiumId(otherCondo));
            units.delete(unit);
            for (String cpf : java.util.List.of("44455566677", "55566677788")) {
                users.findByCpf(cpf).ifPresent(users::delete);
            }
        }
    }

    private UnitBinding registerApplicant(String name, String cpf, Unit unit) throws Exception {
        var request = Map.of("name", name, "cpf", cpf, "password", "123456", "unitId", unit.getId());
        var response = call("POST", "/bindings", null, json.writeValueAsString(request));
        assertEquals(201, response.statusCode(), response.body());
        return bindings.findById(json.readTree(response.body()).get("bindingId").asLong()).orElseThrow();
    }

    @Test void pendingBindingListRequiresManagerAndValidFilter() throws Exception {
        assertEquals(401, call("GET", "/bindings", null, null).statusCode());
        assertEquals(403, call("GET", "/bindings", login("22233344455"), null).statusCode());
        var invalid = call("GET", "/bindings?status=APPROVED", login("11122233344"), null);
        assertEquals(400, invalid.statusCode(), invalid.body());
        assertEquals("status", json.readTree(invalid.body()).get("validationErrors").get(0).get("field").asString());
    }

    @Test void registersAnonymousComplaintsWithDistinctProtocols() throws Exception {
        String resident = login("22233344455");
        Long condo = decoder.decode(resident).<Number>getClaim("condoId").longValue();
        var protocols = new java.util.HashSet<String>();
        for (ComplaintCategory category : ComplaintCategory.values()) {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("category", category.name());
            payload.put("description", "Ocorrência de teste");
            if (category != ComplaintCategory.NOISE) payload.put("location", "Corredor");
            var response = call("POST", "/complaints", resident, json.writeValueAsString(payload));
            assertEquals(201, response.statusCode(), response.body());
            var receipt = json.readTree(response.body());
            assertEquals(2, receipt.size());
            assertEquals("Complaint registered successfully.", receipt.get("message").asString());
            String protocol = receipt.get("protocol").asString();
            assertEquals(4, java.util.UUID.fromString(protocol).version());
            assertTrue(protocols.add(protocol));
            var stored = complaints.findAllByCondominiumId(condo).stream()
                    .filter(c -> c.getProtocol().equals(protocol)).findFirst().orElseThrow();
            assertEquals(category, stored.getCategory());
            assertEquals(payload.get("description"), stored.getDescription());
            assertEquals(payload.get("location"), stored.getLocation());
            assertEquals(ComplaintStatus.RECEIVED, stored.getStatus());
            assertNotNull(stored.getCreatedAt());
        }
    }

    @Test void invalidOrUnauthorizedComplaintsDoNotWriteRows() throws Exception {
        String resident = login("22233344455");
        long before = complaints.count();
        for (Object category : java.util.List.of("INEXISTENTE", 0)) {
            var body = json.writeValueAsString(Map.of("category", category, "description", "Teste"));
            assertEquals(400, call("POST", "/complaints", resident, body).statusCode());
        }
        for (var invalid : java.util.List.of(Map.of("description", "Teste"),
                Map.of("category", "NOISE", "description", "  "),
                Map.of("category", "NOISE", "description", "Teste", "location", "a".repeat(256)))) {
            assertEquals(400, call("POST", "/complaints", resident, json.writeValueAsString(invalid)).statusCode());
        }
        assertEquals(401, call("POST", "/complaints", null, VALID_COMPLAINT).statusCode());
        assertEquals(403, call("POST", "/complaints", login("33344455566"), VALID_COMPLAINT).statusCode());
        assertEquals(403, call("POST", "/complaints", login("11122233344"), VALID_COMPLAINT).statusCode());
        assertEquals(before, complaints.count());
    }

    @Test void invalidExpiredAndTamperedTokens() throws Exception {
        String token = login("11122233344");
        assertEquals(401, call("GET", "/complaints", "invalid", null).statusCode());
        String[] parts = token.split("\\.");
        parts[1] = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1\",\"role\":\"MANAGER\",\"condoId\":999}".getBytes());
        assertEquals(401, call("GET", "/complaints", String.join(".", parts), null).statusCode());
        var claims = JwtClaimsSet.builder().subject("1").issuedAt(Instant.now().minusSeconds(8000)).expiresAt(Instant.now().minusSeconds(1)).claim("role", "MANAGER").claim("condoId", 1).build();
        String expired = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        assertEquals(401, call("GET", "/complaints", expired, null).statusCode());
    }
}



