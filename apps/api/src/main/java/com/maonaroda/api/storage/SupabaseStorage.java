package com.maonaroda.api.storage;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Adaptador interno. O chamador deve autorizar o acesso ao relato antes de chamar. */
@Service
public class SupabaseStorage {
    private final String url, key, bucket;
    private final Duration timeout;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public SupabaseStorage(@Value("${supabase.storage.url:}") String url,
            @Value("${supabase.storage.service-role-key:}") String key,
            @Value("${supabase.storage.bucket:relato-fotos}") String bucket,
            @Value("${supabase.storage.timeout:5s}") Duration timeout) {
        this.url = url.replaceAll("/+$", "");
        this.key = key;
        this.bucket = bucket;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public record Arquivo(String bucket, String caminho, String mimeType, long tamanhoBytes) {}

    public Arquivo enviar(UUID relato, byte[] imagem) {
        if (relato == null) throw new IllegalArgumentException("Relato obrigatório.");
        var foto = Foto.validar(imagem);
        String caminho = "relatos/" + relato + "/" + UUID.randomUUID() + "." + foto.formato();
        executar("POST", "/object/" + bucket + "/" + caminho,
                foto.bytes(), foto.mimeType());
        return new Arquivo(bucket, caminho, foto.mimeType(), foto.bytes().length);
    }

    public URI linkTemporario(String caminho, int segundos) {
        validarCaminho(caminho);
        if (segundos < 1 || segundos > 3600)
            throw new IllegalArgumentException("Validade deve estar entre 1 e 3600 segundos.");
        String resposta = executar("POST", "/object/sign/" + bucket + "/" + caminho,
                ("{\"expiresIn\":" + segundos + "}").getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "application/json");
        try {
            var node = json.readTree(resposta).get("signedURL");
            if (node == null || !node.isString()) throw new IllegalArgumentException();
            String signed = node.asString();
            URI uri = URI.create(signed);
            String expected = "/object/sign/" + bucket + "/" + caminho;
            if (uri.isAbsolute() || !expected.equals(uri.getRawPath()) || uri.getRawAuthority() != null
                    || uri.getRawQuery() == null || !uri.getRawQuery().startsWith("token="))
                throw new IllegalArgumentException();
            return URI.create(url + "/storage/v1" + signed);
        } catch (RuntimeException e) {
            throw new StorageException("Resposta inválida do armazenamento.");
        }
    }

    public void excluir(String caminho) {
        validarCaminho(caminho);
        executar("DELETE", "/object/" + bucket,
                ("{\"prefixes\":[\"" + caminho + "\"]}").getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "application/json");
    }

    private void validarCaminho(String caminho) {
        if (caminho == null || !caminho.matches(
                "relatos/[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.(jpg|png)"))
            throw new IllegalArgumentException("Caminho de foto inválido.");
    }

    private String executar(String method, String path, byte[] body, String type) {
        if (url.isBlank() || key.isBlank())
            throw new StorageException("Armazenamento não configurado.");
        try {
            URI base = URI.create(url);
            boolean local = "http".equals(base.getScheme()) && "127.0.0.1".equals(base.getHost());
            if ((!"https".equals(base.getScheme()) && !local) || base.getHost() == null
                    || base.getRawUserInfo() != null || base.getRawQuery() != null
                    || base.getRawFragment() != null || !base.getPath().isEmpty()
                    || !bucket.matches("[a-z0-9][a-z0-9-]{0,62}"))
                throw new StorageException("Configuração de armazenamento inválida.");
            var request = HttpRequest.newBuilder(URI.create(url + "/storage/v1" + path))
                    .timeout(timeout).header("apikey", key).header("Authorization", "Bearer " + key)
                    .header("Content-Type", type).header("x-upsert", "false")
                    .method(method, HttpRequest.BodyPublishers.ofByteArray(body)).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new StorageException("Operação de armazenamento recusada (HTTP " + response.statusCode() + ").");
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException("Operação de armazenamento interrompida.");
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Não foi possível comunicar com o armazenamento.");
        }
    }
}
