package com.maonaroda.api.rotas;

import com.maonaroda.api.integracao.ors.*;
import com.maonaroda.api.model.rota.*;
import com.maonaroda.api.exception.rota.FalhaRota;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.json.JsonMapper;

class OrsClientTests {
    private HttpServer server;
    private OpenRouteServiceClient client;
    private OrsGeocodingClient busca;
    private final AtomicReference<String> body = new AtomicReference<>(), path = new AtomicReference<>(), authorization = new AtomicReference<>();
    private int status = 200;
    private String resposta = """
        {"features":[{"geometry":{"type":"LineString","coordinates":[[-46.6,-23.5],[-46.601,-23.501]]},
        "properties":{"summary":{"distance":150,"duration":120}}}]}
        """;
    private final PlanejarRotaRequest request = new PlanejarRotaRequest(new Coordenada(-46.6, -23.5), new Coordenada(-46.601, -23.501), "cadeirante");
    @BeforeEach void preparar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", e -> {
            body.set(new String(e.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            path.set(e.getRequestURI().toString()); authorization.set(e.getRequestHeaders().getFirst("Authorization"));
            var bytes = resposta.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            e.sendResponseHeaders(status, bytes.length); e.getResponseBody().write(bytes); e.close();
        }); server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new OpenRouteServiceClient("segredo-ficticio", url);
        busca = new OrsGeocodingClient("busca-ficticia", url);
    }
    @AfterEach void fechar() { client.close(); busca.close(); server.stop(0); }
    @Test void enviaPerfilHeaderEGeoJsonNaOrdemCorreta() {
        var result = client.consultar(request, Duration.ofSeconds(1));
        assertEquals("/wheelchair/geojson", path.get()); assertEquals("segredo-ficticio", authorization.get());
        var json = JsonMapper.builder().build().readTree(body.get());
        assertEquals(-46.6, json.path("coordinates").path(0).path(0).asDouble());
        assertEquals(150, result.getFirst().distancia());
    }
    @Test void trataQuotaSemExporCorpoDoProvedor() {
        status = 429; resposta = "segredo e localização";
        var erro = assertThrows(FalhaRota.class, () -> client.consultar(request, Duration.ofSeconds(1)));
        assertEquals(503, erro.getStatus().value()); assertFalse(erro.getMessage().contains("segredo"));
    }
    @Test void rejeitaRespostaIncompleta() {
        resposta = "{\"features\":[{}]}";
        assertEquals(502, assertThrows(FalhaRota.class, () -> client.consultar(request, Duration.ofSeconds(1))).getStatus().value());
    }
    @Test void retornaListaVaziaSemInventarRota() {
        resposta = "{\"features\":[]}"; assertTrue(client.consultar(request, Duration.ofSeconds(1)).isEmpty());
    }
    @Test void ausenciaDeCaminhoOuPontoNaoViraErroDeIntegracao() {
        status = 404;
        for (int code : new int[]{2009, 2010}) {
            resposta = "{\"error\":{\"code\":" + code + "}}";
            assertTrue(client.consultar(request, Duration.ofSeconds(1)).isEmpty());
        }
    }
    @Test void interrupcaoPreservaSinalDaThread() {
        Thread.currentThread().interrupt();
        try {
            var error = assertThrows(FalhaRota.class, () -> client.consultar(request, Duration.ofSeconds(1)));
            assertEquals(503, error.getStatus().value()); assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }
    @Test void buscaCodificaTextoEMantemChaveForaDaUrl() {
        resposta = """
            {"features":[{"geometry":{"type":"Point","coordinates":[-46.6,-23.5]},
            "properties":{"gid":"pelias:1","label":"Metrô Carrão"}}]}
            """;
        assertEquals("Metrô Carrão", busca.buscar("Metrô Carrão").getFirst().nome());
        assertFalse(path.get().contains("ficticia")); assertEquals("busca-ficticia", authorization.get());
    }
}
