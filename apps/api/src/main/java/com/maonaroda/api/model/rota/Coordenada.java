package com.maonaroda.api.model.rota;

import java.util.List;

public record Coordenada(@com.fasterxml.jackson.annotation.JsonProperty(required = true) double longitude,
        @com.fasterxml.jackson.annotation.JsonProperty(required = true) double latitude) {

    public Coordenada {
        if (!Double.isFinite(longitude)
                || !Double.isFinite(latitude)
                || longitude < -180
                || longitude > 180
                || latitude < -90
                || latitude > 90)
            throw new IllegalArgumentException("Coordenada fora dos limites WGS84.");
    }

    public List<Double> geoJson() {
        return List.of(longitude, latitude);
    }
}
