package com.maonaroda.api.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

import com.maonaroda.api.rotas.ConsultaRota;
import com.maonaroda.api.rotas.FalhaRota;
import com.maonaroda.api.rotas.RotaBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GoogleRoutesService {
    private final String apiKey;
    private final URI endpoint;
    private final Duration timeout;
    private final HttpClient client;
    private final JsonMapper json = JsonMapper.builder().build();

    @jakarta.annotation.PreDestroy
    public void close() { client.close(); }

    public GoogleRoutesService(@Value("${GOOGLE_MAPS_API_KEY:}") String apiKey,
            @Value("${google.routes.url:https://routes.googleapis.com/directions/v2:computeRoutes}") URI endpoint,
            @Value("${google.routes.timeout:8s}") Duration timeout) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    public RotaBase consultar(ConsultaRota consulta) {
        if (consulta == null || consulta.origem() == null || consulta.destino() == null) {
            throw new IllegalArgumentException("Origem e destino são obrigatórios.");
        }
        var body = Map.of("origin", consulta.origem().waypoint(), "destination", consulta.destino().waypoint(),
                "travelMode", "WALK", "languageCode", "pt-BR", "units", "METRIC",
                "computeAlternativeRoutes", false, "polylineEncoding", "ENCODED_POLYLINE");
        if (apiKey.isBlank()) {
            throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "A integração de rotas não está configurada no servidor.");
        }
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "O serviço de rotas atingiu o limite de uso. Tente novamente mais tarde.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // Não propagar corpo, chave, localização ou detalhes internos do provedor.
                throw new FalhaRota(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o Google Routes. Verifique a configuração do serviço.");
            }
            var result = json.readTree(response.body());
            var routes = result.path("routes");
            if (result.isObject() && (routes.isMissingNode() || (routes.isArray() && routes.isEmpty()))) {
                throw new FalhaRota(HttpStatus.NOT_FOUND, "Não foi encontrada uma rota a pé entre os locais informados.");
            }
            var route = routes.path(0);
            var distance = route.path("distanceMeters");
            var duration = route.path("duration").asString("");
            var polyline = route.path("polyline").path("encodedPolyline").asString("");
            if (!routes.isArray() || !distance.isIntegralNumber() || distance.asLong() < 0
                    || !duration.matches("[0-9]+(?:\\.[0-9]{1,9})?s") || polyline.isBlank()) {
                throw new FalhaRota(HttpStatus.BAD_GATEWAY, "O provedor retornou uma rota incompleta ou inválida.");
            }
            double seconds = Double.parseDouble(duration.substring(0, duration.length() - 1));
            if (!Double.isFinite(seconds)) {
                throw new FalhaRota(HttpStatus.BAD_GATEWAY, "O provedor retornou uma duração inválida.");
            }
            return new RotaBase(distance.asLong(), seconds, polyline, "GOOGLE_ROUTES", "WALK", false,
                    "Rota-base sem avaliação de acessibilidade. Rotas a pé do Google estão em beta e podem não incluir calçadas ou caminhos de pedestres. As condições reais podem ser diferentes.");
        } catch (HttpTimeoutException e) {
            throw new FalhaRota(HttpStatus.GATEWAY_TIMEOUT, "O serviço de rotas demorou a responder. Tente novamente.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "A consulta de rota foi interrompida.");
        } catch (IOException | tools.jackson.core.JacksonException e) {
            throw new FalhaRota(HttpStatus.BAD_GATEWAY, "Não foi possível obter uma resposta válida do serviço de rotas.");
        }
    }
}
