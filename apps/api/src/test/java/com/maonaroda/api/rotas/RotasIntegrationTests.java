package com.maonaroda.api.rotas;

import com.maonaroda.api.service.GoogleRoutesService;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RotasIntegrationTests {
    static final HttpServer google;
    static final AtomicInteger status = new AtomicInteger(200);
    static final AtomicInteger calls = new AtomicInteger();
    static final AtomicReference<String> response = new AtomicReference<>();
    static final AtomicReference<String> requestBody = new AtomicReference<>();
    static final AtomicReference<String> key = new AtomicReference<>();
    static final AtomicReference<String> mask = new AtomicReference<>();
    static final JsonMapper json = JsonMapper.builder().build();
    static final String EXEMPLO = """
            {"origem":{"endereco":"Estação de Metrô Carrão, São Paulo, SP"},
             "destino":{"endereco":"UNICID, Rua Cesário Galeno, 448, São Paulo, SP"}}
            """;
    static {
        try {
            google = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            google.createContext("/routes", exchange -> {
                calls.incrementAndGet();
                requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                key.set(exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
                mask.set(exchange.getRequestHeaders().getFirst("X-Goog-FieldMask"));
                var bytes = response.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status.get(), bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            google.start();
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    @LocalServerPort int port;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("google.routes.url", () -> "http://127.0.0.1:" + google.getAddress().getPort() + "/routes");
        registry.add("GOOGLE_MAPS_API_KEY", () -> "chave-ficticia-de-teste");
    }

    @BeforeEach void reset() {
        status.set(200);
        calls.set(0);
        response.set("""
                {"routes":[{"distanceMeters":650,"duration":"510.5s","polyline":{"encodedPolyline":"abc123"}}]}
                """);
    }

    @AfterAll static void stop() { google.stop(0); }

    @Test void consultaAPePorEnderecoSemAfirmarAcessibilidade() throws Exception {
        var result = post(EXEMPLO);
        assertEquals(200, result.statusCode());
        var body = json.readTree(result.body());
        assertEquals(650, body.path("distanciaMetros").asInt());
        assertEquals(510.5, body.path("duracaoSegundos").asDouble());
        assertEquals("abc123", body.path("polylineCodificada").asString());
        assertFalse(body.path("acessibilidadeAvaliada").asBoolean());
        assertTrue(body.path("aviso").asString().contains("beta"));
        var sent = json.readTree(requestBody.get());
        assertEquals("WALK", sent.path("travelMode").asString());
        assertTrue(sent.path("origin").path("address").asString().contains("Carrão"));
        assertEquals("chave-ficticia-de-teste", key.get());
        assertEquals("routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline", mask.get());
        assertFalse(result.body().contains("chave-ficticia"));
    }

    @Test void aceitaCoordenadas() throws Exception {
        assertEquals(200, post("""
                {"origem":{"latitude":-23.55,"longitude":-46.63},
                 "destino":{"latitude":-23.56,"longitude":-46.64}}
                """).statusCode());
        assertEquals(-23.55, json.readTree(requestBody.get()).path("origin").path("location").path("latLng").path("latitude").asDouble());
    }

    @Test void recusaEntradasInvalidasSemChamarGoogle() throws Exception {
        for (var input : new String[]{"{}", "null", "{", """
                {"origem":{"latitude":91,"longitude":0},"destino":{"endereco":"São Paulo"}}
                """, """
                {"origem":{"endereco":" "},"destino":{"endereco":"São Paulo"}}
                """, """
                {"origem":{"endereco":"São Paulo","latitude":0},"destino":{"endereco":"São Paulo"}}
                """}) {
            assertEquals(400, post(input).statusCode(), input);
        }
        assertEquals(0, calls.get());
    }

    @Test void informaAusenciaDeRota() throws Exception {
        response.set("{}");
        assertEquals(404, post(EXEMPLO).statusCode());
    }

    @Test void naoExpoeErroDoProvedorOuCredenciais() throws Exception {
        status.set(403);
        response.set("{\"error\":\"chave-ficticia-de-teste detalhes privados\"}");
        var result = post(EXEMPLO);
        assertEquals(502, result.statusCode());
        assertFalse(result.body().contains("chave-ficticia"));
        assertFalse(result.body().contains("detalhes privados"));
    }

    @Test void trataCotaDoProvedor() throws Exception {
        status.set(429);
        assertEquals(503, post(EXEMPLO).statusCode());
    }

    @Test void rejeitaRespostaIncompletaOuInvalida() throws Exception {
        for (var invalid : new String[]{"not json", "{\"routes\":[{}]}", "[]"}) {
            response.set(invalid);
            assertEquals(502, post(EXEMPLO).statusCode());
        }
    }

    @Test void chaveAusenteNaoConsultaProvedor() {
        var service = new GoogleRoutesService("", URI.create("http://127.0.0.1:" + google.getAddress().getPort() + "/routes"), java.time.Duration.ofSeconds(1));
        var error = assertThrows(FalhaRota.class, () -> service.consultar(new ConsultaRota(
                new PontoRota(null, null, "Carrão"), new PontoRota(null, null, "UNICID"))));
        assertEquals(503, error.status.value());
        assertEquals(0, calls.get());
        service.close();
    }

    @Test void limitaTempoDeEspera() throws Exception {
        var slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/slow", exchange -> {
            try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        slow.start();
        var service = new GoogleRoutesService("teste", URI.create("http://127.0.0.1:" + slow.getAddress().getPort() + "/slow"), java.time.Duration.ofMillis(50));
        try {
            var error = assertThrows(FalhaRota.class, () -> service.consultar(new ConsultaRota(
                    new PontoRota(null, null, "Carrão"), new PontoRota(null, null, "UNICID"))));
            assertEquals(504, error.status.value());
        } finally { service.close(); slow.stop(0); }
    }

    private HttpResponse<String> post(String body) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/rotas/base"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
