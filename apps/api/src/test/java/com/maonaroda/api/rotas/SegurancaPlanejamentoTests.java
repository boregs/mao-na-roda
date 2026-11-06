package com.maonaroda.api.rotas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.*;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SegurancaPlanejamentoTests {
    @LocalServerPort int port;
    private HttpResponse<String> consultar(String path, String token, String body) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
            if (token != null) request.header("Authorization", "Bearer " + token);
            if (body != null) request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    @Test void contaExigeTokenERejeitaTokenInvalido() throws Exception {
        assertEquals(401, consultar("/api/v1/usuarios/me", null, null).statusCode());
        assertEquals(401, consultar("/api/v1/usuarios/me", "invalido", null).statusCode());
    }
    @Test void cadastroELoginSaoPublicosELogoutExigeToken() throws Exception {
        // Sem token: chega ao controller e falha na validação, não na segurança.
        assertEquals(400, consultar("/api/v1/auth/cadastro", null, "{}").statusCode());
        assertEquals(400, consultar("/api/v1/auth/login", null, "{}").statusCode());
        // Sem Supabase configurado nos testes, o login responde indisponível sem vazar detalhes.
        var login = consultar("/api/v1/auth/login", null, "{\"email\":\"a@b.co\",\"senha\":\"x\"}");
        assertTrue(login.statusCode() == 503, login.body());
        assertEquals(401, consultar("/api/v1/auth/logout", null, "").statusCode());
        assertEquals(401, consultar("/api/v1/auth/logout", "invalido", "").statusCode());
    }
    @Test void planejamentoPublicoValidaEntradaSemExigirConta() throws Exception {
        assertEquals(400, consultar("/api/v1/rotas", null, "{}").statusCode());
        var busca = consultar("/api/v1/destinos/busca", null, "{\"texto\":\"Estação Carrão\"}");
        assertEquals(410, busca.statusCode());
        assertTrue(busca.body().contains("POST /api/v1/rotas"));
    }
    @Test void coordenadasAusentesNaoViramZero() throws Exception {
        String json = "{\"origem\":{\"latitude\":-23.5},\"destino\":{\"longitude\":-46.6,\"latitude\":-23.6},\"perfil\":\"cadeirante\"}";
        assertEquals(400, consultar("/api/v1/rotas", null, json).statusCode());
    }
}
