package com.maonaroda.api.rotas;

import com.maonaroda.api.exception.rota.FalhaRota;
import com.maonaroda.api.service.GoogleRoutesService;
import com.maonaroda.api.model.rota.PlanejarRotaRequest;
import com.maonaroda.api.repository.rota.EvidenciaRepository;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Contrato público completo com Google e evidências simulados exclusivamente no teste. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GooglePlanejamentoHttpTests {
    static final AtomicInteger status = new AtomicInteger(200);
    static final AtomicInteger calls = new AtomicInteger();
    static final AtomicReference<String> response = new AtomicReference<>();
    static final AtomicReference<String> requestBody = new AtomicReference<>();
    static final AtomicReference<String> mask = new AtomicReference<>();
    static final AtomicReference<String> key = new AtomicReference<>();
    static final JsonMapper json = JsonMapper.builder().build();
    static final String CANDIDATA = """
            {"distanceMeters":650,"duration":"510.5s","polyline":{"geoJsonLinestring":
            {"type":"LineString","coordinates":[[-46.6,-23.5],[-46.601,-23.501]]}}}
            """;
    static final HttpServer google;
    static {
        try {
            google = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            google.createContext("/routes", e -> {
                calls.incrementAndGet();
                requestBody.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                mask.set(e.getRequestHeaders().getFirst("X-Goog-FieldMask"));
                key.set(e.getRequestHeaders().getFirst("X-Goog-Api-Key"));
                var bytes = response.get().getBytes(StandardCharsets.UTF_8);
                e.getResponseHeaders().set("Content-Type", "application/json");
                e.sendResponseHeaders(status.get(), bytes.length); e.getResponseBody().write(bytes); e.close();
            });
            google.start();
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @LocalServerPort int port;
    @MockitoBean EvidenciaRepository evidencias;
    @DynamicPropertySource static void config(DynamicPropertyRegistry r) {
        r.add("google.routes.url", () -> "http://127.0.0.1:" + google.getAddress().getPort() + "/routes");
        r.add("google.routes.api-key", () -> "chave-ficticia-de-teste");
    }
    @BeforeEach void reset() {
        calls.set(0); status.set(200); response.set("{\"routes\":[" + CANDIDATA + "]}");
        when(evidencias.carregar(anyString())).thenReturn(new EvidenciaRepository.Pacote("SEM_DATASET", "RN-v1", java.util.List.of()));
    }
    @AfterAll static void close() { google.stop(0); }

    @Test
    void planejamentoPublicoUsaGoogleNosTresPerfisComEnderecoOuCoordenadas() throws Exception {
        response.set("{\"routes\":[" + CANDIDATA + "," + CANDIDATA + "]}");
        for (String perfil : java.util.List.of("cadeirante", "muleta_ou_outro_auxilio", "mobilidade_reduzida")) {
            var result = postPlanejamento(perfil, "{\"endereco\":\" Estação Carrão, São Paulo \"}",
                    "{\"latitude\":-23.501,\"longitude\":-46.601}");
            assertEquals(200, result.statusCode(), result.body());
            var body = json.readTree(result.body());
            assertEquals("PARCIAL", body.path("resultado").asString());
            assertEquals("SEM_DATASET", body.path("versaoEvidencias").asString());
            assertEquals(1, body.path("rotas").size(), "Deduplica candidatas iguais do Google");
            var route = body.path("rotas").path(0);
            assertEquals("DESCONHECIDA", route.path("qualidade").path("estado").asString());
            assertEquals(0, route.path("qualidade").path("cobertura").asDouble());
            assertEquals(-46.6, route.path("geometria").path("coordinates").path(0).path(0).asDouble());
            assertTrue(route.path("aviso").asString().contains("Google"));
            assertTrue(body.path("avisos").toString().contains("não garante acessibilidade"));
            assertFalse(result.body().contains("chave-ficticia"));
            var sent = json.readTree(requestBody.get());
            assertEquals("Estação Carrão, São Paulo", sent.path("origin").path("address").asString());
            assertEquals(-23.501, sent.path("destination").path("location").path("latLng").path("latitude").asDouble());
            assertEquals("WALK", sent.path("travelMode").asString());
            assertEquals("GEO_JSON_LINESTRING", sent.path("polylineEncoding").asString());
            assertEquals("HIGH_QUALITY", sent.path("polylineQuality").asString());
            assertTrue(sent.path("computeAlternativeRoutes").asBoolean());
            assertFalse(sent.has("perfil"), "O perfil classifica evidências locais; não é enviado como garantia ao Google");
            assertEquals("routes.duration,routes.distanceMeters,routes.polyline.geoJsonLinestring,routes.legs.steps.distanceMeters,routes.legs.steps.navigationInstruction.instructions,routes.legs.steps.polyline.geoJsonLinestring", mask.get());
            assertEquals("chave-ficticia-de-teste", key.get());
        }
    }

    @Test
    void planejamentoAceitaDoisEnderecosSemGeocoderSeparado() throws Exception {
        response.set("{\"routes\":[" + CANDIDATA + "]}");
        assertEquals(200, postPlanejamento("cadeirante", "{\"endereco\":\"Estação Carrão, São Paulo\"}",
                "{\"endereco\":\"UNICID, São Paulo\"}").statusCode());
        assertEquals("UNICID, São Paulo", json.readTree(requestBody.get()).path("destination").path("address").asString());
    }

    @Test
    void planejamentoSemRotasNaoInventaCandidata() throws Exception {
        for (String empty : java.util.List.of("{}", "{\"routes\":[]}")) {
            response.set(empty);
            var result = postPlanejamento("cadeirante", "{\"latitude\":-23.5,\"longitude\":-46.6}",
                    "{\"latitude\":-23.501,\"longitude\":-46.601}");
            assertEquals(200, result.statusCode());
            assertEquals("SEM_CANDIDATA", json.readTree(result.body()).path("resultado").asString());
            assertTrue(json.readTree(result.body()).path("rotas").isEmpty());
        }
    }

    @Test
    void planejamentoRecusaGeometriaOuMetricasInvalidasDoGoogle() throws Exception {
        for (String invalid : java.util.List.of("", "null", "[]", "{\"routes\":null}", "{\"routes\":[{}]}",
                "{\"routes\":[" + CANDIDATA.replace("LineString", "Point") + "]}",
                "{\"routes\":[" + CANDIDATA.replace("-23.5]", "-91.5]") + "]}",
                "{\"routes\":[" + CANDIDATA.replace("510.5s", "-1s") + "]}",
                "{\"routes\":[" + CANDIDATA.replace("650", "-1") + "]}")) {
            response.set(invalid);
            var result = postPlanejamento("cadeirante", "{\"endereco\":\"Carrão\"}", "{\"endereco\":\"UNICID\"}");
            assertEquals(502, result.statusCode(), invalid);
        }
    }

    @Test
    void planejamentoValidaEntradaAntesDeConsumirQuota() throws Exception {
        for (String origin : java.util.List.of("{}", "{\"latitude\":-23.5}",
                "{\"endereco\":\"\"}", "{\"endereco\":\"Carrão\",\"longitude\":-46.6}")) {
            assertEquals(400, postPlanejamento("cadeirante", origin, "{\"endereco\":\"UNICID\"}").statusCode());
        }
        assertEquals(400, postPlanejamento("invalido", "{\"endereco\":\"Carrão\"}", "{\"endereco\":\"UNICID\"}").statusCode());
        assertEquals(0, calls.get());
    }

    @Test
    void planejamentoRespeitaOrcamentoRestanteENaoConsultaSemChave() {
        var request = new PlanejarRotaRequest(new PontoRota(null, null, "Carrão"), new PontoRota(null, null, "UNICID"), "cadeirante");
        var uri = URI.create("http://127.0.0.1:" + google.getAddress().getPort() + "/routes");
        var service = new GoogleRoutesService("teste", uri, java.time.Duration.ofSeconds(8));
        var semChave = new GoogleRoutesService("", uri, java.time.Duration.ofSeconds(8));
        try {
            assertEquals(504, assertThrows(FalhaRota.class,
                    () -> service.consultarCandidatas(request, java.time.Duration.ZERO)).getStatus().value());
            assertEquals(503, assertThrows(FalhaRota.class,
                    () -> semChave.consultarCandidatas(request, java.time.Duration.ofSeconds(8))).getStatus().value());
            assertEquals(0, calls.get());
        } finally { service.close(); semChave.close(); }
    }

    @Test
    void mantemEtapasNaCandidataCorrespondente() throws Exception {
        String primeira = candidataComEtapa(-46.601, "Vire à esquerda");
        String segunda = candidataComEtapa(-46.602, "Vire à direita");
        response.set("{\"routes\":[" + primeira + "," + segunda + "]}");
        var result = postPlanejamento("cadeirante", "{\"endereco\":\"Carrão\"}", "{\"endereco\":\"UNICID\"}");
        assertEquals(200, result.statusCode(), result.body());
        var rotas = json.readTree(result.body()).path("rotas");
        assertEquals(2, rotas.size());
        for (var rota : rotas) {
            var etapa = rota.path("etapas").path(0);
            var fim = rota.path("geometria").path("coordinates").path(1);
            assertEquals(fim, etapa.path("geometria").path("coordinates").path(1));
            assertEquals(fim.path(0).asDouble() == -46.601 ? "Vire à esquerda" : "Vire à direita",
                    etapa.path("instrucao").asString());
        }
    }

    private String candidataComEtapa(double longitude, String instrucao) {
        String linha = """
                {"geoJsonLinestring":{"type":"LineString","coordinates":[[-46.6,-23.5],[%s,-23.501]]}}
                """.formatted(longitude);
        return """
                {"distanceMeters":650,"duration":"510.5s","polyline":%s,"legs":[{"steps":[
                {"distanceMeters":650,"navigationInstruction":{"instructions":"%s"},"polyline":%s}]}]}
                """.formatted(linha, instrucao, linha);
    }

    private HttpResponse<String> postPlanejamento(String perfil, String origem, String destino) throws Exception {
        return post("/api/v1/rotas", "{\"origem\":" + origem + ",\"destino\":" + destino + ",\"perfil\":\"" + perfil + "\"}");
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }
}
