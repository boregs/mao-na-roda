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
    public record LocalRota(Ponto coordenada, String endereco) {
        public LocalRota {
            if ((coordenada == null) == (endereco == null)) throw new IllegalArgumentException("Informe um endereço ou coordenadas.");
            if (endereco != null) {
                endereco = endereco.trim();
                if (endereco.length() < 3 || endereco.length() > 200)
                    throw new IllegalArgumentException("Informe um endereço entre 3 e 200 caracteres.");
            }
        }
        public static LocalRota deTexto(String texto) {
            String valor = texto == null ? "" : texto.trim();
            Ponto ponto = Ponto.manual(valor);
            if (ponto != null) return new LocalRota(ponto, null);
            var partes = valor.split(",", -1);
            if (partes.length == 2) try {
                Double.parseDouble(partes[0].trim()); Double.parseDouble(partes[1].trim());
                throw new IllegalArgumentException("Latitude ou longitude fora dos limites.");
            } catch (NumberFormatException ignored) { /* Um endereço também pode conter vírgula. */ }
            return new LocalRota(null, valor);
        }
    }
    public record Etapa(String instrucao, List<Ponto> pontos) { }
    public record Evidencia(double inicio, double fim, String descricao) { }
    public record Rota(String id, List<Ponto> pontos, double distancia, double duracao,
            String qualidade, double cobertura, int impeditivas, int atencao, String detalhes,
            List<Etapa> etapas, List<Evidencia> evidencias) {
        public Rota(String id, List<Ponto> pontos, double distancia, double duracao, String qualidade,
                double cobertura, int impeditivas, int atencao, String detalhes) {
            this(id, pontos, distancia, duracao, qualidade, cobertura, impeditivas, atencao,
                    detalhes, List.of(), List.of());
        }
    }
    public record Consulta(String resultado, List<Rota> rotas, String aviso) { }
}
