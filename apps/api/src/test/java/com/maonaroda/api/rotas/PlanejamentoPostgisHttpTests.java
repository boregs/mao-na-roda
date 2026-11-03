package com.maonaroda.api.rotas;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.*;
import java.net.http.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "TEST_POSTGIS_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlanejamentoPostgisHttpTests {
    static HttpServer google;
    @LocalServerPort int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) throws Exception {
        google = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        google.createContext("/", e -> {
            var body = """
                {"routes":[{"distanceMeters":150,"duration":"120s",
                "polyline":{"geoJsonLinestring":{"type":"LineString","coordinates":[[-46.6,-23.5],[-46.601,-23.501]]}}}]}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            e.sendResponseHeaders(200, body.length); e.getResponseBody().write(body); e.close();
        }); google.start();
        r.add("spring.datasource.url", () -> System.getenv("TEST_POSTGIS_URL"));
        r.add("spring.datasource.username", () -> System.getenv("TEST_POSTGIS_USER"));
        r.add("spring.datasource.password", () -> System.getenv("TEST_POSTGIS_PASSWORD"));
        r.add("google.routes.api-key", () -> "fixture-sem-custo");
        r.add("google.routes.url", () -> "http://127.0.0.1:" + google.getAddress().getPort());
    }
    @AfterAll static void close() { if (google != null) google.stop(0); }
    @Test void visitanteConsultaCatalogoEPlanejaNosTresPerfis() throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            String base = "http://localhost:" + port + "/api/v1";
            var catalogo = http.send(HttpRequest.newBuilder(URI.create(base + "/perfis-mobilidade")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, catalogo.statusCode());
            var json = JsonMapper.builder().build(); var perfis = json.readTree(catalogo.body()); assertEquals(3, perfis.size());
            for (var perfil : perfis) {
                String body = "{\"origem\":{\"endereco\":\"Estação Carrão, São Paulo\"},\"destino\":{\"longitude\":-46.601,\"latitude\":-23.501},\"perfil\":\"" + perfil.path("codigo").asString() + "\"}";
                var response = http.send(HttpRequest.newBuilder(URI.create(base + "/rotas")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                var result = json.readTree(response.body());
                assertEquals("PARCIAL", result.path("resultado").asString());
                assertEquals("DESCONHECIDA", result.path("rotas").path(0).path("qualidade").path("estado").asString());
                assertTrue(result.path("avisos").toString().contains("Google"));
            }
        }
    }
}
