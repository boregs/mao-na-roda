package com.maonaroda.mao_na_roda.data;

import java.util.List;

public final class DadosRotas {
    private DadosRotas() { }
    public record Ponto(double longitude, double latitude) {
        public Ponto {
            if (!Double.isFinite(longitude) || !Double.isFinite(latitude) || Math.abs(longitude) > 180 || Math.abs(latitude) > 90)
                throw new IllegalArgumentException("Coordenada inválida.");
        }
        // Entrada manual: latitude, longitude. GeoJSON e API usam longitude, latitude.
        public static Ponto manual(String texto) {
            String[] partes = texto.trim().split(",");
            if (partes.length != 2) return null;
            try { return new Ponto(Double.parseDouble(partes[1].trim()), Double.parseDouble(partes[0].trim())); }
            catch (IllegalArgumentException e) { return null; }
        }
    }
    public record Perfil(String codigo, String nome) { }
    public record Destino(String id, String nome, Ponto coordenada) { }
    public record Rota(String id, List<Ponto> pontos, double distancia, double duracao,
            String qualidade, double cobertura, int impeditivas, int atencao, String detalhes) { }
    public record Consulta(String resultado, List<Rota> rotas, String aviso) { }
}
