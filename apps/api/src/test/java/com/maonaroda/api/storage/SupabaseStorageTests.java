package com.maonaroda.api.storage;

import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class SupabaseStorageTests {
    HttpServer server;
    SupabaseStorage storage;
    AtomicInteger calls = new AtomicInteger();
    AtomicInteger status = new AtomicInteger(200);
    AtomicReference<String> response = new AtomicReference<>("{}");
    String method, path, auth, key, type, upsert;
    byte[] body;
    static final String CAMINHO = "relatos/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".png";

    @BeforeEach void preparar() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            method = exchange.getRequestMethod(); path = exchange.getRequestURI().getPath();
            auth = exchange.getRequestHeaders().getFirst("Authorization");
            key = exchange.getRequestHeaders().getFirst("apikey");
            type = exchange.getRequestHeaders().getFirst("Content-Type");
            upsert = exchange.getRequestHeaders().getFirst("x-upsert");
            body = exchange.getRequestBody().readAllBytes();
            byte[] bytes = response.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        storage = new SupabaseStorage("http://127.0.0.1:" + server.getAddress().getPort(),
                "chave-ficticia", "relato-fotos", Duration.ofSeconds(2));
    }
    @AfterEach void parar() { server.stop(0); }

    byte[] foto(String format) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }

    @Test void enviaPngComCaminhoSeguroEHeaders() throws Exception {
        UUID relato = UUID.randomUUID();
        var arquivo = storage.enviar(relato, foto("png"));
        assertEquals("relato-fotos", arquivo.bucket());
        assertTrue(arquivo.caminho().startsWith("relatos/" + relato + "/"));
        assertEquals("/storage/v1/object/relato-fotos/" + arquivo.caminho(), path);
        assertEquals("POST", method); assertEquals("image/png", type);
        assertEquals("Bearer chave-ficticia", auth); assertEquals("chave-ficticia", key);
        assertEquals("false", upsert); assertEquals(body.length, arquivo.tamanhoBytes());
        assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(body)));
    }
    @Test void aceitaJpeg() throws Exception {
        var arquivo = storage.enviar(UUID.randomUUID(), foto("jpeg"));
        assertEquals("image/jpeg", arquivo.mimeType()); assertTrue(arquivo.caminho().endsWith(".jpg"));
    }
    @Test void rejeitaArquivosInvalidosAntesDaRede() {
        for (byte[] dados : new byte[][] {new byte[0], new byte[] {1,2,3}, new byte[5_000_001]})
            assertThrows(IllegalArgumentException.class, () -> storage.enviar(UUID.randomUUID(), dados));
        assertEquals(0, calls.get());
    }
    @Test void geraLinkTemporario() {
        response.set("{\"signedURL\":\"/object/sign/relato-fotos/" + CAMINHO + "?token=ficticio\"}");
        URI link = storage.linkTemporario(CAMINHO, 60);
        assertEquals("POST", method);
        assertEquals("/storage/v1/object/sign/relato-fotos/" + CAMINHO, path);
        assertEquals(60, JsonMapper.builder().build().readTree(body).get("expiresIn").asInt());
        assertEquals("token=ficticio", link.getQuery());
    }
    @Test void excluiSomenteObjetoSolicitado() {
        storage.excluir(CAMINHO);
        assertEquals("DELETE", method); assertEquals("/storage/v1/object/relato-fotos", path);
        assertEquals(CAMINHO, JsonMapper.builder().build().readTree(body).get("prefixes").get(0).asString());
    }
    @Test void recusaTraversalEValidadeInvalida() {
        assertThrows(IllegalArgumentException.class, () -> storage.excluir("../segredo"));
        assertThrows(IllegalArgumentException.class, () -> storage.linkTemporario(CAMINHO, 3601));
        assertEquals(0, calls.get());
    }
    @Test void rejeitaRespostaAssinadaMalformadaOuExterna() {
        for (String valor : new String[] {"{}", "invalido", "{\"signedURL\":\"https://externo.test/foto\"}"}) {
            response.set(valor);
            assertThrows(StorageException.class, () -> storage.linkTemporario(CAMINHO, 60));
        }
    }
    @Test void naoExpoeRespostaRemotaNemChave() {
        response.set("segredo remoto chave-ficticia");
        for (int code : new int[] {302, 401, 404, 409, 429, 500}) {
            status.set(code);
            var ex = assertThrows(StorageException.class, () -> storage.excluir(CAMINHO));
            assertFalse(ex.getMessage().contains("segredo"));
            assertFalse(ex.getMessage().contains("chave-ficticia")); assertNull(ex.getCause());
        }
    }
    @Test void configuracaoAusenteNaoChamaRede() {
        var vazio = new SupabaseStorage("", "", "relato-fotos", Duration.ofSeconds(1));
        assertThrows(StorageException.class, () -> vazio.excluir(CAMINHO));
        assertEquals(0, calls.get());
    }
    @Test void falhaDeConexaoETimeoutSaoSanitizados() {
        var lento = new SupabaseStorage("http://127.0.0.1:" + server.getAddress().getPort(),
                "chave-ficticia", "relato-fotos", Duration.ofMillis(100));
        server.removeContext("/");
        server.createContext("/", exchange -> {
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        var ex = assertThrows(StorageException.class, () -> lento.excluir(CAMINHO));
        assertFalse(ex.getMessage().contains("chave-ficticia"));
        server.stop(0);
        assertThrows(StorageException.class, () -> storage.excluir(CAMINHO));
    }
}
