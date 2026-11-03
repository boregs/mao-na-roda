package com.maonaroda.api.rotas;

import com.maonaroda.api.integracao.rotas.*;
import com.maonaroda.api.model.rota.*;
import com.maonaroda.api.repository.rota.EvidenciaRepository;
import com.maonaroda.api.repository.rota.EvidenciaRepository.*;
import com.maonaroda.api.service.rota.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlanejamentoTests {
    private final CandidataRota rota = new CandidataRota(List.of(List.of(-46.6, -23.5), List.of(-46.601, -23.501)), 150, 120);
    private final AvaliadorAcessibilidade avaliador = new AvaliadorAcessibilidade();
    private List<Evidencia> completo(CandidataRota r, String trecho, double inicio, double fim, String classificacao) {
        var rows = new ArrayList<Evidencia>();
        for (int i = 0; i < 10; i++) rows.add(new Evidencia(r.assinatura(), trecho, inicio, fim, "tipo_" + i,
                classificacao, i == 0 ? "presente" : "ausente", "dados_publicos", OffsetDateTime.now()));
        return rows;
    }
    @Test void dadosAusentesNaoGarantemAcessibilidade() {
        var resposta = avaliador.avaliar(rota, List.of());
        assertEquals("DESCONHECIDA", resposta.qualidade().estado());
        assertEquals(0, resposta.qualidade().cobertura());
        assertEquals(0, resposta.barreiras().impeditivas());
        assertTrue(resposta.qualidade().trechosDesconhecidos() > 0);
    }
    @Test void coberturaNaoDuplicaSobreposicoesEExibeImpedimento() {
        var rows = new ArrayList<>(completo(rota, "a", 0, .8, "impeditiva"));
        rows.addAll(completo(rota, "b", .5, 1, "atencao"));
        var resposta = avaliador.avaliar(rota, rows);
        assertEquals(1, resposta.qualidade().cobertura());
        assertEquals("DOCUMENTADA", resposta.qualidade().estado());
        assertEquals(1, resposta.barreiras().impeditivas());
        assertFalse(resposta.aviso().contains("Rota acessível"));
    }
    @Test void dataDesconhecidaOuAntigaNaoContaComoCobertura() {
        var rows = completo(rota, "a", 0, 1, "impeditiva").stream().map(e -> new Evidencia(e.assinatura(), e.trecho(),
                e.inicio(), e.fim(), e.tipo(), e.classificacao(), e.condicao(), e.fonte(), null)).toList();
        assertEquals("DESCONHECIDA", avaliador.avaliar(rota, rows).qualidade().estado());
        assertTrue(avaliador.avaliar(rota, rows).trechos().getFirst().barreiras().getFirst().desatualizada());
    }
    @Test void outraCalcadaOuDirecaoNaoHerdaEvidencias() {
        var paralela = new CandidataRota(List.of(List.of(-46.60001, -23.5), List.of(-46.60101, -23.501)), 150, 120);
        var inversa = new CandidataRota(rota.coordenadas().reversed(), 150, 120);
        var associador = new AssociadorEspacial();
        assertTrue(associador.associar(paralela, completo(rota, "a", 0, 1, "atencao")).isEmpty());
        assertTrue(associador.associar(inversa, completo(rota, "a", 0, 1, "atencao")).isEmpty());
    }
    @Test void rankingPriorizaDocumentacaoDeduplicaELimita() {
        var curta = new CandidataRota(List.of(List.of(-46.6, -23.5), List.of(-46.602, -23.501)), 100, 90);
        var repo = new EvidenciaRepository(null) {
            @Override public Pacote carregar(String perfil) { return new Pacote("piloto-1", "RN-v1", completo(rota, "a", 0, 1, "impeditiva")); }
        };
        var service = new PlanejadorRotasService((r, timeout) -> List.of(curta, rota, rota), repo, new AssociadorEspacial(), avaliador);
        var resposta = service.planejar(new PlanejarRotaRequest(new Coordenada(-46.6, -23.5), new Coordenada(-46.601, -23.501), "cadeirante"));
        assertEquals(2, resposta.rotas().size());
        assertEquals(rota.assinatura(), resposta.rotas().getFirst().id());
        assertEquals("PARCIAL", resposta.resultado());
        assertEquals("piloto-1", resposta.versaoEvidencias());
    }
    @Test void validaPerfilCoordenadaEDestino() {
        assertThrows(IllegalArgumentException.class, () -> new Coordenada(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Coordenada(0, 91));
        assertThrows(IllegalArgumentException.class, () -> new PlanejarRotaRequest(new Coordenada(0, 0), new Coordenada(1, 1), "visual"));
        for (String perfil : List.of("cadeirante", "muleta_ou_outro_auxilio", "mobilidade_reduzida"))
            assertDoesNotThrow(() -> new PlanejarRotaRequest(new Coordenada(0, 0), new Coordenada(1, 1), perfil));
    }
    @Test void orcamentoIncluiTempoGastoAntesDoProvedor() {
        var tempo = new java.util.concurrent.atomic.AtomicLong(0);
        var orcamento = new OrcamentoConsulta(Duration.ofSeconds(8), tempo::get);
        tempo.set(Duration.ofSeconds(2).toNanos());
        assertEquals(Duration.ofSeconds(6), orcamento.restante());
        tempo.set(Duration.ofSeconds(8).toNanos());
        assertEquals(504, assertThrows(com.maonaroda.api.exception.rota.FalhaRota.class, orcamento::restante).getStatus().value());
    }
    @Test void quintaConsultaConcorrenteNaoChamaProvedor() throws Exception {
        var iniciadas = new java.util.concurrent.CountDownLatch(4);
        var liberar = new java.util.concurrent.CountDownLatch(1);
        var repo = new EvidenciaRepository(null) {
            @Override public Pacote carregar(String perfil) { return new Pacote("SEM_DATASET", "RN-v1", List.of()); }
        };
        ProvedorRotas lento = (request, timeout) -> {
            iniciadas.countDown();
            try { liberar.await(3, java.util.concurrent.TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return List.of(rota);
        };
        var service = new PlanejadorRotasService(lento, repo, new AssociadorEspacial(), avaliador);
        var request = new PlanejarRotaRequest(new Coordenada(-46.6, -23.5), new Coordenada(-46.601, -23.501), "cadeirante");
        try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 4; i++) workers.submit(() -> service.planejar(request));
            try {
                assertTrue(iniciadas.await(3, java.util.concurrent.TimeUnit.SECONDS));
                assertEquals(429, assertThrows(com.maonaroda.api.exception.rota.FalhaRota.class, () -> service.planejar(request)).getStatus().value());
            } finally { liberar.countDown(); }
        }
    }
}
