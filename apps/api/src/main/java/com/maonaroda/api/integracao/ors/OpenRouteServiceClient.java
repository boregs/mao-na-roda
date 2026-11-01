package com.maonaroda.api.integracao.ors;

import com.maonaroda.api.integracao.rotas.*;
import com.maonaroda.api.model.rota.*;
import com.maonaroda.api.exception.rota.FalhaRota;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Component
public class OpenRouteServiceClient implements ProvedorRotas {
    private final String key;
    private final String url;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final JsonMapper json = JsonMapper.builder().build();
    public OpenRouteServiceClient(@Value("${ors.api-key:}") String key,
            @Value("${ors.routing-url:https://api.openrouteservice.org/v2/directions}") String url) {
        this.key = key; this.url = url.replaceAll("/+$", "");
    }
    @jakarta.annotation.PreDestroy public void close() { http.close(); }
    @Override public List<CandidataRota> consultar(PlanejarRotaRequest request, Duration timeout) {
        if (key.isBlank()) throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "O serviço ORS não está configurado.");
        String profile = request.perfil().equals("cadeirante") ? "wheelchair" : "foot-walking";
        var payload = new LinkedHashMap<String, Object>();
        payload.put("coordinates", List.of(request.origem().geoJson(), request.destino().geoJson()));
        payload.put("instructions", false);
        payload.put("alternative_routes", Map.of("target_count", 3));
        try {
            var query = HttpRequest.newBuilder(URI.create(url + "/" + profile + "/geojson"))
                    .timeout(timeout).header("Authorization", key).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
            var response = http.send(query, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "Limite do provedor atingido. Tente mais tarde.");
            if (response.body().length() > 4_000_000) throw new IllegalArgumentException("Resposta excessiva.");
            if (response.statusCode() != 200) {
                int code = json.readTree(response.body()).path("error").path("code").asInt(-1);
                if (code == 2009 || code == 2010) return List.of();
                throw new FalhaRota(HttpStatus.BAD_GATEWAY, "Não foi possível consultar rotas no ORS.");
            }
            var features = json.readTree(response.body()).path("features");
            if (!features.isArray()) throw new IllegalArgumentException("GeoJSON incompleto.");
            var routes = new ArrayList<CandidataRota>();
            for (var feature : features) {
                if (routes.size() == 6) break;
                var geometry = feature.path("geometry");
                if (!"LineString".equals(geometry.path("type").asString())) throw new IllegalArgumentException("Tipo inválido.");
                var points = new ArrayList<List<Double>>();
                for (var point : geometry.path("coordinates")) {
                    if (!point.isArray() || point.size() < 2 || !point.path(0).isNumber() || !point.path(1).isNumber())
                        throw new IllegalArgumentException("Coordenada inválida.");
                    points.add(List.of(point.path(0).asDouble(), point.path(1).asDouble()));
                }
                var summary = feature.path("properties").path("summary");
                if (!summary.path("distance").isNumber() || !summary.path("duration").isNumber())
                    throw new IllegalArgumentException("Resumo incompleto.");
                routes.add(new CandidataRota(points, summary.path("distance").asDouble(), summary.path("duration").asDouble()));
            }
            return List.copyOf(routes);
        } catch (HttpTimeoutException e) {
            throw new FalhaRota(HttpStatus.GATEWAY_TIMEOUT, "O ORS demorou a responder.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "A consulta foi interrompida.");
        } catch (java.io.IOException | tools.jackson.core.JacksonException | IllegalArgumentException e) {
            throw new FalhaRota(HttpStatus.BAD_GATEWAY, "Resposta de rotas inválida ou indisponível.");
        }
    }
}
