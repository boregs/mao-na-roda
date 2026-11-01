package com.maonaroda.api.service.rota;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Service;
import com.maonaroda.api.integracao.rotas.*;
import com.maonaroda.api.model.rota.*;
import com.maonaroda.api.model.rota.ConsultaRotasResponse.RotaAvaliadaResponse;
import com.maonaroda.api.repository.rota.EvidenciaRepository;
import com.maonaroda.api.exception.rota.FalhaRota;
import org.springframework.http.HttpStatus;

@Service
public class PlanejadorRotasService {
    private final ProvedorRotas provedor;
    private final EvidenciaRepository evidencias;
    private final AssociadorEspacial associador;
    private final AvaliadorAcessibilidade avaliador;
    private final Semaphore simultaneas = new Semaphore(4);
    public PlanejadorRotasService(ProvedorRotas provedor, EvidenciaRepository evidencias,
            AssociadorEspacial associador, AvaliadorAcessibilidade avaliador) {
        this.provedor = provedor; this.evidencias = evidencias; this.associador = associador; this.avaliador = avaliador;
    }
    public ConsultaRotasResponse planejar(PlanejarRotaRequest request) {
        if (!simultaneas.tryAcquire()) throw new FalhaRota(HttpStatus.TOO_MANY_REQUESTS, "Há muitas consultas. Tente novamente em instantes.");
        var orcamento = new OrcamentoConsulta(Duration.ofSeconds(8));
        try {
            // Carregar evidências antes da chamada paga: banco indisponível não consome quota ORS.
            var pacote = evidencias.carregar(request.perfil());
            var candidatas = provedor.consultar(request, orcamento.restante());
            var unicas = new LinkedHashMap<String, CandidataRota>();
            candidatas.stream().limit(6).forEach(c -> unicas.putIfAbsent(c.assinatura(), c));
            var rotas = unicas.values().stream().map(c -> avaliador.avaliar(c, associador.associar(c, pacote.evidencias())))
                    .sorted(Comparator.comparingInt((RotaAvaliadaResponse r) -> switch (r.qualidade().estado()) {
                        case "DOCUMENTADA" -> 0; case "PARCIAL" -> 1; default -> 2;
                    }).thenComparingInt(r -> r.barreiras().impeditivas()).thenComparingInt(r -> r.barreiras().atencao())
                    .thenComparing(Comparator.comparingDouble((RotaAvaliadaResponse r) -> r.qualidade().cobertura()).reversed())
                    .thenComparingDouble(RotaAvaliadaResponse::distanciaMetros).thenComparingDouble(RotaAvaliadaResponse::duracaoSegundos))
                    .limit(3).toList();
            orcamento.restante();
            boolean parcial = rotas.stream().anyMatch(r -> !r.qualidade().estado().equals("DOCUMENTADA"));
            return new ConsultaRotasResponse(rotas.isEmpty() ? "SEM_CANDIDATA" : parcial ? "PARCIAL" : "CONCLUIDA",
                    pacote.matriz(), pacote.versao(), rotas, List.of(AvaliadorAcessibilidade.AVISO));
        } finally { simultaneas.release(); }
    }
}
