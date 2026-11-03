package com.maonaroda.api.integracao.rotas;

import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import com.maonaroda.api.model.rota.Coordenada;

public record CandidataRota(List<List<Double>> coordenadas, double distancia, double duracao) {
    public CandidataRota {
        if (coordenadas == null
                || coordenadas.size() < 2
                || coordenadas.size() > 50000
                || !Double.isFinite(distancia)
                || !Double.isFinite(duracao)
                || distancia <= 0
                || duracao <= 0)
            throw new IllegalArgumentException("Geometria ou resumo de rota inválido.");

        coordenadas = coordenadas.stream().map(p -> {
            if (p.size() < 2)
                throw new IllegalArgumentException("Coordenada incompleta.");

            return new Coordenada(p.get(0), p.get(1)).geoJson();
        }).toList();

        if (coordenadas.stream().distinct().limit(2).count() < 2)
            throw new IllegalArgumentException("A rota precisa de dois pontos distintos.");
    }
    public String assinatura() {
        // Não arredondar: outra geometria exige nova revisão; não associar calçadas por proximidade.
        String canonical = coordenadas.stream().map(p -> Double.toString(p.get(0)) + "," + Double.toString(p.get(1)))
                .collect(java.util.stream.Collectors.joining(";"));

        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
