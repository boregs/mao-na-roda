package com.maonaroda.mao_na_roda.data;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;
import org.json.*;
import com.maonaroda.mao_na_roda.data.DadosRotas.*;

public final class ApiClient implements RotasRepository {
    private final String base;
    private final boolean depuracao;
    private final Executor callback;
    private final ExecutorService tarefas = Executors.newFixedThreadPool(2);
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private final Set<Chamada<?>> chamadas = ConcurrentHashMap.newKeySet();
    public ApiClient(String base, Executor callback) { this(base, callback, false); }
    public ApiClient(String base, Executor callback, boolean depuracao) {
        this.base = base.replaceAll("/+$", ""); this.callback = callback; this.depuracao = depuracao;
    }
    private interface Leitura<T> { T ler(String json) throws JSONException; }
    private final class Chamada<T> implements Cancelavel {
        final AtomicBoolean encerrada = new AtomicBoolean();
        volatile HttpURLConnection conexao;
        volatile Future<?> tarefa, prazo;
        @Override public void cancelar() {
            if (encerrada.compareAndSet(false, true)) limpar();
        }
        void limpar() {
            if (conexao != null) conexao.disconnect();
            if (tarefa != null) tarefa.cancel(true);
            if (prazo != null) prazo.cancel(false);
            chamadas.remove(this);
        }
        void concluir(T valor, String erro, Retorno<T> retorno) {
            if (!encerrada.compareAndSet(false, true)) return;
            if (prazo != null) prazo.cancel(false);
            chamadas.remove(this);
            callback.execute(() -> { if (erro == null) retorno.sucesso(valor); else retorno.falha(erro); });
        }
    }
    private <T> Cancelavel executar(String path, JSONObject body, Leitura<T> leitura, Retorno<T> retorno) {
        Chamada<T> chamada = new Chamada<>(); chamadas.add(chamada);
        chamada.prazo = timer.schedule(() -> {
            chamada.concluir(null, "A consulta demorou a responder. Tente novamente.", retorno);
            chamada.limpar();
        }, 9, TimeUnit.SECONDS);
        chamada.tarefa = tarefas.submit(() -> {
            try {
                if (!PoliticaEnderecoApi.permitida(base, depuracao)) throw new IllegalStateException();
                if (chamada.encerrada.get()) return;
                var connection = (HttpURLConnection) new URL(base + path).openConnection(); chamada.conexao = connection;
                if (chamada.encerrada.get()) { connection.disconnect(); return; }
                connection.setConnectTimeout(2000); connection.setReadTimeout(8500);
                connection.setInstanceFollowRedirects(false); connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json, application/problem+json");
                if (body != null) {
                    connection.setRequestMethod("POST"); connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (var stream = connection.getOutputStream()) { stream.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
                }
                int status = connection.getResponseCode();
                var stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                if (stream == null) throw new java.io.IOException();
                String json;
                try (stream; var bytes = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192]; int size;
                    while ((size = stream.read(buffer)) != -1) {
                        if (bytes.size() + size > 4_000_000) throw new java.io.IOException();
                        bytes.write(buffer, 0, size);
                    }
                    json = bytes.toString(StandardCharsets.UTF_8.name());
                }
                if (status < 200 || status >= 300) {
                    String mensagem = status == 429 ? "Limite de consultas atingido. Aguarde um minuto."
                            : status == 503 ? "O serviço está indisponível. Tente novamente mais tarde."
                            : status == 504 ? "A consulta demorou a responder. Tente novamente."
                            : "Não foi possível concluir a consulta. Confira os locais e tente novamente.";
                    chamada.concluir(null, mensagem, retorno);
                } else chamada.concluir(leitura.ler(json), null, retorno);
            } catch (IllegalStateException e) {
                chamada.concluir(null, "O endereço da API ainda não foi configurado ou é inválido.", retorno);
            } catch (Exception e) {
                chamada.concluir(null, "Não foi possível conectar. Verifique sua conexão e tente novamente.", retorno);
            } finally { if (chamada.conexao != null) chamada.conexao.disconnect(); }
        });
        return chamada;
    }
    private static JSONObject local(LocalRota local) throws JSONException {
        if (local.coordenada() == null) return new JSONObject().put("endereco", local.endereco());
        var p = local.coordenada();
        return new JSONObject().put("longitude", p.longitude()).put("latitude", p.latitude());
    }
    public static JSONObject planejamentoJson(LocalRota origem, LocalRota destino, String perfil) throws JSONException {
        return new JSONObject().put("origem", local(origem)).put("destino", local(destino)).put("perfil", perfil);
    }
    @Override public Cancelavel perfis(Retorno<List<Perfil>> retorno) {
        return executar("/api/v1/perfis-mobilidade", null, json -> {
            var array = new JSONArray(json); var list = new ArrayList<Perfil>();
            for (int i = 0; i < array.length(); i++) { var p = array.getJSONObject(i); list.add(new Perfil(p.getString("codigo"), p.getString("nome"))); }
            return List.copyOf(list);
        }, retorno);
    }
    @Override public Cancelavel planejar(LocalRota origem, LocalRota destino, String perfil, Retorno<Consulta> retorno) {
        try {
            return executar("/api/v1/rotas", planejamentoJson(origem, destino, perfil), ApiClient::lerConsulta, retorno);
        } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }
    public static Consulta lerConsulta(String json) throws JSONException {
        var root = new JSONObject(json); var array = root.getJSONArray("rotas"); var list = new ArrayList<Rota>();
        for (int i = 0; i < Math.min(3, array.length()); i++) {
            var r = array.getJSONObject(i); var pontos = new ArrayList<Ponto>();
            var geometry = r.getJSONObject("geometria");
            if (!"LineString".equals(geometry.getString("type"))) throw new JSONException("Geometria inválida.");
            var coords = geometry.getJSONArray("coordinates");
            if (coords.length() < 2 || coords.length() > 50000) throw new JSONException("Geometria inválida.");
            for (int j = 0; j < coords.length(); j++) { var p = coords.getJSONArray(j); pontos.add(new Ponto(p.getDouble(0), p.getDouble(1))); }
            var qualidade = r.getJSONObject("qualidade"); var barreiras = r.getJSONObject("barreiras");
            var evidencias = new ArrayList<Evidencia>();
            var details = new StringBuilder(); var trechos = r.getJSONArray("trechos");
            for (int j = 0; j < trechos.length(); j++) {
                var trecho = trechos.getJSONObject(j);
                var items = trecho.getJSONArray("barreiras");
                for (int k = 0; k < items.length(); k++) {
                    var b = items.getJSONObject(k);
                    if ("ausente".equals(b.optString("condicao"))) continue;
                    details.append(b.optString("tipo").replace('_', ' ')).append("\nImpacto se presente: ")
                            .append(b.optString("classificacao", "desconhecida").replace("atencao", "atenção"))
                            .append("\nCondição: ").append(b.optString("condicao")).append("\nFonte: ")
                            .append("openstreetmap".equals(b.optString("fonte")) ? "OpenStreetMap" : "Dados públicos")
                            .append("\nVerificação: ").append(b.isNull("verificadoEm") ? "data desconhecida" : data(b.getString("verificadoEm")))
                            .append(b.optBoolean("desatualizada") ? " · requer nova verificação" : "");
                    if (!b.isNull("referenciaFonte") && !b.optString("referenciaFonte").isEmpty())
                        details.append("\nReferência: ").append(b.optString("referenciaFonte"));
                    if (!b.isNull("motivoSemVerificacao") && !b.optString("motivoSemVerificacao").isEmpty())
                        details.append("\n").append(b.optString("motivoSemVerificacao"));
                    details.append("\n\n");
                    double inicio = trecho.optDouble("inicio", -1);
                    double fim = trecho.optDouble("fim", -1);
                    if ("presente".equals(b.optString("condicao")) && inicio >= 0 && fim > inicio && fim <= 1) {
                        evidencias.add(new Evidencia(inicio, fim, b.optString("tipo").replace('_', ' ')
                                + (b.optBoolean("desatualizada") ? " (requer nova verificação)" : ""),
                                "impeditiva".equals(b.optString("classificacao"))));
                    }
                }
            }
            if (!r.optString("aviso").isEmpty()) details.append("\n").append(r.optString("aviso"));
            var etapas = new ArrayList<Etapa>();
            var passos = r.optJSONArray("etapas");
            if (passos != null) for (int j = 0; j < passos.length(); j++) {
                var passo = passos.getJSONObject(j);
                var linha = passo.getJSONObject("geometria").getJSONArray("coordinates");
                var pontosEtapa = new ArrayList<Ponto>();
                for (int k = 0; k < linha.length(); k++) {
                    var ponto = linha.getJSONArray(k);
                    pontosEtapa.add(new Ponto(ponto.getDouble(0), ponto.getDouble(1)));
                }
                if (pontosEtapa.size() >= 2) etapas.add(new Etapa(passo.optString("instrucao"), List.copyOf(pontosEtapa)));
            }
            list.add(new Rota(r.getString("id"), List.copyOf(pontos), r.getDouble("distanciaMetros"), r.getDouble("duracaoSegundos"),
                    qualidade.getString("estado"), qualidade.getDouble("cobertura"), barreiras.getInt("impeditivas"), barreiras.getInt("atencao"),
                    details.length() == 0 ? "Nenhuma barreira conhecida informada. Isso não garante acessibilidade." : details.toString(),
                    List.copyOf(etapas), List.copyOf(evidencias)));
        }
        var avisos = root.optJSONArray("avisos"); var texto = new StringBuilder();
        if (avisos != null) for (int i = 0; i < avisos.length(); i++) {
            if (i > 0) texto.append("\n\n"); texto.append(avisos.getString(i));
        }
        if (texto.length() == 0) texto.append("Sugestão baseada nos dados disponíveis. As condições reais podem ser diferentes.");
        return new Consulta(root.getString("resultado"), List.copyOf(list), texto.toString());
    }
    @Override public void close() {
        chamadas.forEach(Chamada::cancelar); tarefas.shutdownNow(); timer.shutdownNow();
    }
    private static String data(String value) {
        try {
            return java.time.OffsetDateTime.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", java.util.Locale.forLanguageTag("pt-BR")));
        } catch (java.time.format.DateTimeParseException e) { return "data desconhecida"; }
    }
}
