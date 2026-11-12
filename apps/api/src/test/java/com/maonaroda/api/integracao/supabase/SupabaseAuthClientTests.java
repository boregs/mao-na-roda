package com.maonaroda.api.integracao.supabase;

import com.maonaroda.api.exception.auth.FalhaAutenticacao;
import com.maonaroda.api.exception.auth.FalhaAutenticacao.Tipo;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class SupabaseAuthClientTests {
    HttpServer server;
    SupabaseAuthClient client;
    AtomicInteger status = new AtomicInteger(200);
    AtomicReference<String> response = new AtomicReference<>("{}");
    String method, path, query, key, auth, body;
    final java.util.List<String> chamadas = new java.util.concurrent.CopyOnWriteArrayList<>();
    static final String SESSAO = "{\"access_token\":\"jwt-ficticio\",\"token_type\":\"bearer\","
            + "\"expires_in\":900,\"refresh_token\":\"refresh-que-nao-sai-da-api\",\"user\":{\"id\":\"x\"}}";

    @BeforeEach void preparar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            method = exchange.getRequestMethod();
            path = exchange.getRequestURI().getPath();
            query = exchange.getRequestURI().getQuery();
            key = exchange.getRequestHeaders().getFirst("apikey");
            auth = exchange.getRequestHeaders().getFirst("Authorization");
            body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            chamadas.add(method + " " + path + " " + auth);
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        client = new SupabaseAuthClient("http://127.0.0.1:" + server.getAddress().getPort() + "/",
                "anon-ficticia", Duration.ofSeconds(2));
    }

    @AfterEach void parar() { server.stop(0); client.close(); }

    @Test void cadastroEnviaNomesComoMetadadosEDevolveSessaoSemRefreshToken() {
        response.set(SESSAO);
        var sessao = client.cadastrar("ana@example.com", "senha12345", "Ana", "Silva").orElseThrow();
        assertEquals("POST", method);
        assertEquals("/auth/v1/signup", path);
        assertEquals("anon-ficticia", key);
        assertNull(auth);
        var enviado = JsonMapper.builder().build().readTree(body);
        assertEquals("ana@example.com", enviado.path("email").asString());
        assertEquals("senha12345", enviado.path("password").asString());
        assertEquals("Ana", enviado.path("data").path("nome").asString());
        assertEquals("Silva", enviado.path("data").path("sobrenome").asString());
        assertEquals("jwt-ficticio", sessao.accessToken());
        assertEquals("Bearer", sessao.tokenType());
        assertEquals(900, sessao.expiraEmSegundos());
        assertFalse(sessao.toString().contains("refresh"));
    }

    @Test void cadastroSemSessaoIndicaConfirmacaoPendente() {
        response.set("{\"id\":\"x\",\"email\":\"ana@example.com\"}");
        assertTrue(client.cadastrar("ana@example.com", "senha12345", "Ana", "Silva").isEmpty());
    }

    @Test void errosDeCadastroSaoTraduzidos() {
        assertCadastro(422, "{\"code\":422,\"error_code\":\"user_already_exists\",\"msg\":\"x\"}", Tipo.CONTA_EXISTENTE);
        assertCadastro(422, "{\"error_code\":\"email_exists\"}", Tipo.CONTA_EXISTENTE);
        assertCadastro(422, "{\"error_code\":\"weak_password\"}", Tipo.SENHA_FRACA);
        assertCadastro(400, "{\"error_code\":\"email_address_invalid\"}", Tipo.EMAIL_INVALIDO);
        assertCadastro(429, "{\"error_code\":\"over_request_rate_limit\"}", Tipo.LIMITE_PROVEDOR);
        assertCadastro(500, "erro sem json", Tipo.INDISPONIVEL);
    }

    @Test void loginUsaGrantDeSenha() {
        response.set(SESSAO);
        var sessao = client.entrar("ana@example.com", "senha12345");
        assertEquals("/auth/v1/token", path);
        assertEquals("grant_type=password", query);
        assertEquals("jwt-ficticio", sessao.accessToken());
    }

    @Test void errosDeLoginSaoTraduzidos() {
        assertLogin(400, "{\"error_code\":\"invalid_credentials\"}", Tipo.CREDENCIAIS_INVALIDAS);
        assertLogin(400, "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid login credentials\"}",
                Tipo.CREDENCIAIS_INVALIDAS);
        assertLogin(400, "{\"error_code\":\"email_not_confirmed\"}", Tipo.EMAIL_NAO_CONFIRMADO);
        assertLogin(429, "{}", Tipo.LIMITE_PROVEDOR);
        assertLogin(503, "{}", Tipo.INDISPONIVEL);
        assertLogin(200, "{\"user\":{}}", Tipo.INDISPONIVEL);
    }

    @Test void logoutEnviaTokenDoUsuarioEAceitaSessaoJaEncerrada() {
        status.set(204); response.set("");
        client.sair("jwt-do-usuario");
        assertEquals("/auth/v1/logout", path);
        assertEquals("scope=local", query);
        assertEquals("Bearer jwt-do-usuario", auth);
        status.set(401);
        assertDoesNotThrow(() -> client.sair("jwt-do-usuario"));
    }

    @Test void confirmarSenhaDevolveDonoEEncerraSessaoDeConfirmacao() {
        var dono = java.util.UUID.randomUUID();
        response.set("{\"access_token\":\"jwt-confirmacao\",\"expires_in\":900,\"user\":{\"id\":\"" + dono + "\"}}");
        assertEquals(dono, client.confirmarSenha("ana@example.com", "senha12345"));
        assertEquals(java.util.List.of("POST /auth/v1/token null", "POST /auth/v1/logout Bearer jwt-confirmacao"), chamadas);
    }

    @Test void confirmarSenhaTraduzErros() {
        status.set(400); response.set("{\"error_code\":\"invalid_credentials\"}");
        assertEquals(Tipo.CREDENCIAIS_INVALIDAS,
                assertThrows(FalhaAutenticacao.class, () -> client.confirmarSenha("a@b.co", "x")).tipo());
        status.set(200); response.set("{\"access_token\":\"t\",\"user\":{}}");
        assertEquals(Tipo.INDISPONIVEL,
                assertThrows(FalhaAutenticacao.class, () -> client.confirmarSenha("a@b.co", "x")).tipo());
    }

    private SupabaseAuthClient comChaveDeServico(String chave) {
        return new SupabaseAuthClient("http://127.0.0.1:" + server.getAddress().getPort(),
                "anon-ficticia", chave, Duration.ofSeconds(2));
    }

    @Test void exclusaoUsaRotaAdministrativaEChaveDeServico() {
        status.set(200);
        var usuario = java.util.UUID.randomUUID();
        var legado = comChaveDeServico("eyJchave-de-servico");
        legado.excluirUsuario(usuario);
        assertEquals("DELETE", method);
        assertEquals("/auth/v1/admin/users/" + usuario, path);
        assertEquals("eyJchave-de-servico", key);
        assertEquals("Bearer eyJchave-de-servico", auth);
        legado.close();
        // As chaves secretas novas não são JWT: vão só no apikey.
        var nova = comChaveDeServico("sb_secret_ficticia");
        nova.excluirUsuario(usuario);
        assertEquals("sb_secret_ficticia", key);
        assertNull(auth);
        nova.close();
    }

    @Test void exclusaoDeUsuarioJaRemovidoNaoEhErro() {
        var cliente = comChaveDeServico("sb_secret_ficticia");
        status.set(404); response.set("{\"error_code\":\"user_not_found\"}");
        assertDoesNotThrow(() -> cliente.excluirUsuario(java.util.UUID.randomUUID()));
        status.set(500);
        var erro = assertThrows(FalhaAutenticacao.class, () -> cliente.excluirUsuario(java.util.UUID.randomUUID()));
        assertEquals(Tipo.INDISPONIVEL, erro.tipo());
        cliente.close();
    }

    @Test void exclusaoSemChaveDeServicoNaoChamaRede() {
        method = null;
        var erro = assertThrows(FalhaAutenticacao.class, () -> client.excluirUsuario(java.util.UUID.randomUUID()));
        assertEquals(Tipo.INDISPONIVEL, erro.tipo());
        assertNull(method);
    }

    @Test void semConfiguracaoNaoChamaRede() {
        var semConfig = new SupabaseAuthClient("", "", Duration.ofSeconds(1));
        var erro = assertThrows(FalhaAutenticacao.class, () -> semConfig.entrar("a@b.c", "x"));
        assertEquals(Tipo.INDISPONIVEL, erro.tipo());
        semConfig.close();
    }

    private void assertCadastro(int codigo, String corpo, Tipo esperado) {
        status.set(codigo); response.set(corpo);
        var erro = assertThrows(FalhaAutenticacao.class,
                () -> client.cadastrar("ana@example.com", "senha12345", "Ana", "Silva"));
        assertEquals(esperado, erro.tipo());
    }

    private void assertLogin(int codigo, String corpo, Tipo esperado) {
        status.set(codigo); response.set(corpo);
        var erro = assertThrows(FalhaAutenticacao.class, () -> client.entrar("ana@example.com", "senha12345"));
        assertEquals(esperado, erro.tipo());
    }
}
