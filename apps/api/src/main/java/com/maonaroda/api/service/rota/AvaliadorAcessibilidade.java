package com.maonaroda.api.service.rota;

import java.time.*;
import java.util.*;
import com.maonaroda.api.integracao.rotas.CandidataRota;
import com.maonaroda.api.model.rota.ConsultaRotasResponse.*;
import com.maonaroda.api.repository.rota.EvidenciaRepository.Evidencia;
import org.springframework.stereotype.Component;

@Component
public class AvaliadorAcessibilidade {
    public static final String AVISO = "Sugestão baseada nos dados disponíveis. Dados ausentes não garantem acessibilidade; as condições reais podem ser diferentes.";

    public RotaAvaliadaResponse avaliar(CandidataRota rota, List<Evidencia> evidencias) {
        var grupos = new LinkedHashMap<String, List<Evidencia>>();
        evidencias.forEach(e -> grupos.computeIfAbsent(e.trecho(), k -> new ArrayList<>()).add(e));
        var trechos = new ArrayList<TrechoAvaliadoResponse>();
        var intervalos = new ArrayList<double[]>();
        int impeditivas = 0, atencao = 0, desconhecidos = 0;
        var agora = OffsetDateTime.now(ZoneOffset.UTC);
        var limite = agora.minusMonths(1);

        for (var grupo : grupos.entrySet()) {
            var rows = grupo.getValue(); var primeiro = rows.getFirst();
            var barreiras = new ArrayList<BarreiraResponse>();

            long conhecidas = rows.stream().filter(e -> e.tipo() != null && e.classificacao() != null
                    && Set.of("presente", "ausente").contains(e.condicao() == null ? "" : e.condicao())
                    && e.verificadoEm() != null && !e.verificadoEm().isBefore(limite)
                    && !e.verificadoEm().isAfter(agora)).map(Evidencia::tipo).distinct().count();

            boolean completo = conhecidas == 10;

            if (completo) {
                intervalos.add(new double[]{primeiro.inicio(), primeiro.fim()});
            }
            else {
                desconhecidos++;
            }

            for (var e : rows) {
                if (e.tipo() == null)
                    continue;

                boolean antigo = e.verificadoEm() == null || e.verificadoEm().isBefore(limite) || e.verificadoEm().isAfter(agora);

                barreiras.add(new BarreiraResponse(e.tipo(), e.classificacao(), e.condicao(), e.fonte(),
                        e.verificadoEm() == null ? null : e.verificadoEm().toString(), antigo,
                        e.referenciaFonte(), e.versaoFonte(), e.motivoSemVerificacao()));
                if ("presente".equals(e.condicao())) {
                    if ("impeditiva".equals(e.classificacao()))
                        impeditivas++;

                    if ("atencao".equals(e.classificacao()))
                        atencao++;
                }
            }
            trechos.add(new TrechoAvaliadoResponse(grupo.getKey(), primeiro.inicio(), primeiro.fim(),
                    completo ? "DOCUMENTADA" : "DESCONHECIDA", List.copyOf(barreiras)));
        }

        intervalos.sort(Comparator.comparingDouble(a -> a[0]));
        double cobertura = 0, fim = 0;
        for (var i : intervalos) {
            cobertura += Math.max(0, i[1] - Math.max(fim, i[0])); fim = Math.max(fim, i[1]);
        }

        cobertura = Math.min(1, cobertura);

        String estado = cobertura >= 0.999999 ? "DOCUMENTADA" : cobertura > 0 ? "PARCIAL" : "DESCONHECIDA";

        if (cobertura < 0.999999) desconhecidos = Math.max(1, desconhecidos);

        return new RotaAvaliadaResponse(rota.assinatura(), new Geometria(rota.coordenadas()), rota.distancia(), rota.duracao(),
                new ResumoBarreiras(impeditivas, atencao), new QualidadeEvidencia(estado, cobertura, desconhecidos),
                List.copyOf(trechos), AVISO, rota.etapas());
    }
}
