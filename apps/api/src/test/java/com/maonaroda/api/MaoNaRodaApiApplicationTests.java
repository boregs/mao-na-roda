package com.maonaroda.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MaoNaRodaApiApplicationTests {
    @LocalServerPort
    int port;

    @Test
    void informaSaudeSemExporDetalhes() throws Exception {
        var response = get("/actuator/health");
        assertEquals(200, response.statusCode());
        var body = JsonMapper.builder().build().readTree(response.body());
        assertEquals("UP", body.path("status").asString());
        assertFalse(body.has("components"));
        assertFalse(body.has("details"));
    }

    @Test
    void publicaContratoOpenApiEInterfaceSwagger() throws Exception {
        var response = get("/v3/api-docs");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"openapi\""));
        assertTrue(response.body().contains("/actuator/health"));
        assertEquals(200, get("/swagger-ui/index.html").statusCode());
    }

    @Test
    void naoExpoeConfiguracoesDoAmbiente() throws Exception {
        assertEquals(404, get("/actuator/env").statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

}
