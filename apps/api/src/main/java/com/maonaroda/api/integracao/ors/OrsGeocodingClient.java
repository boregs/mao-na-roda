package com.maonaroda.api.integracao.ors;

import com.maonaroda.api.exception.rota.FalhaRota;
import com.maonaroda.api.model.rota.Coordenada;
import com.maonaroda.api.integracao.busca.ProvedorBusca;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Component
public class OrsGeocodingClient implements ProvedorBusca {
    private final String key, url;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    public OrsGeocodingClient(@Value("${ors.geocoding-key:}") String key,
            @Value("${ors.geocoding-url:https://api.openrouteservice.org/geocode/search}") String url) {
        this.key = key; this.url = url;
    }
    @jakarta.annotation.PreDestroy public void close() { http.close(); }
    @Override public List<Destino> buscar(String texto) {
        if (texto == null || texto.isBlank() || texto.strip().length() < 3 || texto.length() > 200)
            throw new IllegalArgumentException("Informe entre 3 e 200 caracteres para buscar um local.");
        if (key.isBlank()) throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "A busca de locais não está configurada.");
        try {
            // Authorization evita expor a chave na URL e em logs de proxies.
            var uri = URI.create(url + "?text=" + URLEncoder.encode(texto.strip(), java.nio.charset.StandardCharsets.UTF_8) + "&size=5&boundary.country=BR");
            var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(4)).header("Authorization", key).GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "Limite de buscas atingido. Tente mais tarde.");
            if (response.statusCode() != 200 || response.body().length() > 1_000_000) throw new IllegalArgumentException();
            var features = JsonMapper.builder().build().readTree(response.body()).path("features");
            if (!features.isArray()) throw new IllegalArgumentException();
            var result = new ArrayList<Destino>();
            for (var f : features) {
                if (result.size() == 5) break;
                var p = f.path("properties"); var c = f.path("geometry").path("coordinates");
                if (!"Point".equals(f.path("geometry").path("type").asString())
                        || !c.path(0).isNumber() || !c.path(1).isNumber() || p.path("label").asString("").isBlank())
                    throw new IllegalArgumentException();
                result.add(new Destino(p.path("gid").asString(""), p.path("label").asString(), new Coordenada(c.path(0).asDouble(), c.path(1).asDouble())));
            }
            return List.copyOf(result);
        } catch (HttpTimeoutException e) {
            throw new FalhaRota(HttpStatus.GATEWAY_TIMEOUT, "A busca demorou a responder.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new FalhaRota(HttpStatus.SERVICE_UNAVAILABLE, "Busca interrompida.");
        } catch (java.io.IOException | tools.jackson.core.JacksonException | IllegalArgumentException e) {
            throw new FalhaRota(HttpStatus.BAD_GATEWAY, "Não foi possível buscar locais.");
        }
    }
}
