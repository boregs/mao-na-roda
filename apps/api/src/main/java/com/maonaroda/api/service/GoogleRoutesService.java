package com.maonaroda.api.service;

import com.maonaroda.api.exception.rota.FalhaRota;
import com.maonaroda.api.integracao.rotas.CandidataRota;
import com.maonaroda.api.model.rota.PlanejarRotaRequest;
import com.maonaroda.api.rotas.ConsultaRota;
import com.maonaroda.api.rotas.RotaBase;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Service
public class GoogleRoutesService {
    public static final String AVISO_CAMINHADA = "Rota a pé do Google em beta: pode não incluir calçadas ou caminhos de pedestres. "
            + "O Google não garante acessibilidade para o perfil selecionado; confira as evidências e as condições reais.";
    private static final String MASCARA_BASE = "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline";
    private static final String MASCARA_CANDIDATAS = "routes.duration,routes.distanceMeters,routes.polyline.geoJsonLinestring";
    private final String apiKey;
    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    public GoogleRoutesService(
            @Value("${google.routes.api-key:${GOOGLE_ROUTES_API_KEY:${GOOGLE_MAPS_API_KEY:}}}") String apiKey,
            @Value("${google.routes.url:https://routes.googleapis.com/directions/v2:computeRoutes}") URI endpoint,
            @Value("${google.routes.timeout:8s}") Duration timeout) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.timeout = timeout;
    }

    @PreDestroy public void close() { client.close(); }

    /** Preserva o contrato existente de /rotas/base e sua polyline codificada. */
    public RotaBase consultar(ConsultaRota consulta) {
        var routes = enviar(consulta, timeout, false);
        if (routes.isEmpty()) throw new FalhaRota(HttpStatus.NOT_FOUND,
                "Não foi encontrada uma rota a pé entre os locais informados.");
        var route = routes.getFirst();
        var distance = distancia(route);
        var duration = duracao(route);
        var polyline = route.path("polyline").path("encodedPolyline").asString("");
        if (polyline.isBlank()) throw respostaInvalida();
        return new RotaBase(distance, duration, polyline, "GOOGLE_ROUTES", "WALK", false,
                "Rota-base sem avaliação de acessibilidade. " + AVISO_CAMINHADA);
    }

    /** Usa endereços ou coordenadas no próprio Routes, sem geocoder separado. */
    public List<CandidataRota> consultarCandidatas(PlanejarRotaRequest consulta, Duration restante) {
        var routes = enviar(new ConsultaRota(consulta.origem(), consulta.destino()), restante, true);
        var result = new ArrayList<CandidataRota>();
        for (var route : routes) {
            var geometry = route.path("polyline").path("geoJsonLinestring");
            var coordinates = geometry.path("coordinates");
            if (!"LineString".equals(geometry.path("type").asString()) || !coordinates.isArray()
                    || coordinates.size() < 2 || coordinates.size() > 50_000) throw respostaInvalida();
            var pontos = new ArrayList<List<Double>>();
            for (var point : coordinates) {
                if (!point.isArray() || point.size() < 2 || !point.path(0).isNumber() || !point.path(1).isNumber())
                    throw respostaInvalida();
                pontos.add(List.of(point.path(0).asDouble(), point.path(1).asDouble()));
            }
            try {
                result.add(new CandidataRota(pontos, distancia(route), duracao(route)));
            } catch (IllegalArgumentException e) { throw respostaInvalida(); }
        }
        return List.copyOf(result);
    }

    private List<JsonNode> enviar(ConsultaRota consulta, Duration restante, boolean alternativas) {
        if (consulta == null || consulta.origem() == null || consulta.destino() == null)
            throw new IllegalArgumentException("Origem e destino são obrigatórios.");
        var body = new LinkedHashMap<String, Object>();
        body.put("origin", consulta.origem().waypoint());
        body.put("destination", consulta.destino().waypoint());
        body.put("travelMode", "WALK");
        body.put("languageCode", "pt-BR");
        body.put("regionCode", "br");
        body.put("units", "METRIC");
        body.put("computeAlternativeRoutes", alternativas);
        body.put("polylineEncoding", alternativas ? "GEO_JSON_LINESTRING" : "ENCODED_POLYLINE");
        if (alternativas) body.put("polylineQuality", "HIGH_QUALITY");
        if (apiKey.isBlank()) throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE,
                "O Google Routes não está configurado no servidor.");
        Duration limite = restante.compareTo(timeout) < 0 ? restante : timeout;
        if (limite.isZero() || limite.isNegative()) throw new FalhaRota(HttpStatus.GATEWAY_TIMEOUT,
                "O planejamento excedeu o tempo de espera.");
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(limite)
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", alternativas ? MASCARA_CANDIDATAS : MASCARA_BASE)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE,
                    "O serviço de rotas atingiu o limite de uso. Tente novamente mais tarde.");
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new FalhaRota(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o Google Routes. Verifique a configuração do serviço.");
            if (response.body().length() > 4_000_000) throw respostaInvalida();
            var root = json.readTree(response.body());
            if (root == null || !root.isObject()) throw respostaInvalida();
            var routes = root.path("routes");
            if (routes.isMissingNode()) return List.of();
            if (!routes.isArray()) throw respostaInvalida();
            var result = new ArrayList<JsonNode>();
            for (var route : routes) {
                if (result.size() == 6) break;
                result.add(route);
            }
            return result;
        } catch (HttpTimeoutException e) {
            throw new FalhaRota(HttpStatus.GATEWAY_TIMEOUT, "O serviço de rotas demorou a responder. Tente novamente.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "A consulta de rota foi interrompida.");
        } catch (IOException | tools.jackson.core.JacksonException e) {
            // Nunca propagar corpo de erro, chave ou endereços do provedor.
            throw new FalhaRota(HttpStatus.BAD_GATEWAY, "Não foi possível obter uma resposta válida do serviço de rotas.");
        }
    }

    private static long distancia(JsonNode route) {
        var distance = route.path("distanceMeters");
        if (!distance.isIntegralNumber() || !distance.canConvertToLong() || distance.asLong() < 0) throw respostaInvalida();
        return distance.asLong();
    }

    private static double duracao(JsonNode route) {
        String duration = route.path("duration").asString("");
        if (!duration.matches("[0-9]+(?:\\.[0-9]{1,9})?s")) throw respostaInvalida();
        double seconds = Double.parseDouble(duration.substring(0, duration.length() - 1));
        if (!Double.isFinite(seconds)) throw respostaInvalida();
        return seconds;
    }

    private static FalhaRota respostaInvalida() {
        return new FalhaRota(HttpStatus.BAD_GATEWAY, "O provedor retornou uma rota incompleta ou inválida.");
    }
}
