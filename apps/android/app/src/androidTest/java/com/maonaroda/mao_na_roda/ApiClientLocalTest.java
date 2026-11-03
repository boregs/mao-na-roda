package com.maonaroda.mao_na_roda;

import android.security.NetworkSecurityPolicy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.maonaroda.mao_na_roda.data.ApiClient;
import com.maonaroda.mao_na_roda.data.DadosRotas.*;
import com.maonaroda.mao_na_roda.data.RotasRepository.Retorno;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ApiClientLocalTest {
    @Test public void debugConsultaApiLocalPorHttpSemLiberarOutrosHosts() throws Exception {
        assertTrue(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("127.0.0.1"));
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("api.example.com"));
        var pedidos = new LinkedBlockingQueue<String>();
        var worker = Executors.newSingleThreadExecutor();
        try (var server = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"));
             var client = new ApiClient("http://127.0.0.1:" + server.getLocalPort(), Runnable::run, true)) {
            server.setSoTimeout(10_000);
            var atendimentos = worker.submit(() -> {
                try {
                    for (int i = 0; i < 2; i++) try (var socket = server.accept()) {
                        socket.setSoTimeout(5_000);
                        var input = socket.getInputStream(); var headers = new ByteArrayOutputStream();
                        String linhas;
                        do {
                            int b = input.read(); if (b < 0 || headers.size() > 16_384) throw new IOException("Cabeçalho inválido");
                            headers.write(b); linhas = headers.toString("UTF-8");
                        } while (!linhas.endsWith("\r\n\r\n"));
                        int tamanho = 0;
                        for (String linha : linhas.split("\r\n")) if (linha.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:"))
                            tamanho = Integer.parseInt(linha.substring(linha.indexOf(':') + 1).trim());
                        if (tamanho > 8192) throw new IOException("Corpo muito grande");
                        var corpo = new byte[tamanho];
                        for (int n = 0; n < tamanho;) {
                            int lidos = input.read(corpo, n, tamanho - n); if (lidos < 0) throw new EOFException(); n += lidos;
                        }
                        pedidos.add(linhas.split("\r\n")[0] + "\n" + new String(corpo, StandardCharsets.UTF_8));
                        String resposta = i == 0 ? "[{\"codigo\":\"cadeirante\",\"nome\":\"Cadeirante\"}]"
                                : "{\"resultado\":\"SEM_CANDIDATA\",\"rotas\":[],\"avisos\":[\"Rota a pé do Google não garante acessibilidade.\"]}";
                        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
                        var output = socket.getOutputStream();
                        output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: "
                                + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        output.write(bytes); output.flush();
                    }
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
            var perfis = new CompletableFuture<List<Perfil>>(); client.perfis(retorno(perfis));
            assertEquals("cadeirante", perfis.get(10, TimeUnit.SECONDS).get(0).codigo());
            var consulta = new CompletableFuture<Consulta>();
            client.planejar(LocalRota.deTexto("Estação Carrão, São Paulo"), LocalRota.deTexto("-23.501, -46.601"), "cadeirante", retorno(consulta));
            var resultado = consulta.get(10, TimeUnit.SECONDS);
            assertTrue(resultado.rotas().isEmpty()); assertTrue(resultado.aviso().contains("Google"));
            assertTrue(pedidos.remove().startsWith("GET /api/v1/perfis-mobilidade "));
            String pedido = pedidos.remove(); assertTrue(pedido.startsWith("POST /api/v1/rotas "));
            var json = new JSONObject(pedido.substring(pedido.indexOf('\n') + 1));
            assertEquals("Estação Carrão, São Paulo", json.getJSONObject("origem").getString("endereco"));
            assertEquals(-23.501, json.getJSONObject("destino").getDouble("latitude"), 0);
            atendimentos.get(5, TimeUnit.SECONDS);
        } finally { worker.shutdownNow(); }
    }
    private static <T> Retorno<T> retorno(CompletableFuture<T> future) {
        return new Retorno<>() {
            @Override public void sucesso(T value) { future.complete(value); }
            @Override public void falha(String error) { future.completeExceptionally(new AssertionError(error)); }
        };
    }
}
