package com.maonaroda.api.rotas;

import java.util.Map;

public record PontoRota(Double latitude, Double longitude, String endereco) {
    public Map<String, Object> waypoint() {
        if (endereco != null) {
            if (endereco.isBlank() || endereco.length() > 500 || latitude != null || longitude != null) {
                throw new IllegalArgumentException(
                        "Informe um endereço de até 500 caracteres ou latitude e longitude, sem misturar os formatos.");
            }
            return Map.of("address", endereco.trim());
        }
        if (latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Informe latitude entre -90 e 90 e longitude entre -180 e 180, ou um endereço.");
        }
        return Map.of("location", Map.of("latLng", Map.of("latitude", latitude, "longitude", longitude)));
    }
}
