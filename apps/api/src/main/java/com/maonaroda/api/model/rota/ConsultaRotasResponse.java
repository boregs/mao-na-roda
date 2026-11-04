package com.maonaroda.api.model.rota;

import java.util.List;

public record ConsultaRotasResponse(String resultado, String versaoMatriz, String versaoEvidencias, List<RotaAvaliadaResponse> rotas, List<String> avisos) {
    public record Geometria(String type, List<List<Double>> coordinates) {
        public Geometria(List<List<Double>> coordinates) { this("LineString", coordinates); }
    }

    public record QualidadeEvidencia(String estado, double cobertura, int trechosDesconhecidos) { }
    public record ResumoBarreiras(int impeditivas, int atencao) { }
    public record TrechoAvaliadoResponse(String id, double inicio, double fim, String qualidade,
            List<BarreiraResponse> barreiras) { }

    public record BarreiraResponse(String tipo, String classificacao, String condicao, String fonte,
            String verificadoEm, boolean desatualizada, String referenciaFonte, String versaoFonte,
            String motivoSemVerificacao) { }

    public record EtapaResponse(String instrucao, int distanciaMetros, Geometria geometria) { }

    public record RotaAvaliadaResponse(String id, Geometria geometria, double distanciaMetros,
            double duracaoSegundos, ResumoBarreiras barreiras, QualidadeEvidencia qualidade,
            List<TrechoAvaliadoResponse> trechos, String aviso, List<EtapaResponse> etapas) { }
}
