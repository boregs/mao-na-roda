package com.maonaroda.mao_na_roda;

import com.maonaroda.mao_na_roda.data.DadosRotas.*;
import com.maonaroda.mao_na_roda.mapa.Percurso;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PercursoTest {
    private final Ponto origem = ponto(0);
    private final Ponto meio = ponto(50);
    private final Ponto fim = ponto(100);

    private static Ponto ponto(double metros) {
        return new Ponto(metros / 111_195, 0);
    }

    private Percurso percurso() { return percurso(false); }

    @Test public void encontraRelatoPertoDoTrechoSemContarPontosDistantes() {
        var linha = List.of(origem, fim);
        assertTrue(Percurso.pertoDaRota(new Ponto(meio.longitude(), 10.0 / 111_195), linha));
        assertFalse(Percurso.pertoDaRota(new Ponto(meio.longitude(), 20.0 / 111_195), linha));
        assertFalse(Percurso.pertoDaRota(ponto(120), linha));
    }

    @Test public void recomendaAlternativaQueEvitaBarreirasLocaisESemImpedimentosDaApi() {
        var principal = candidata("principal", 0, 0);
        var alternativa = candidata("alternativa", 30, 0);
        var bloqueada = candidata("bloqueada", 60, 1);
        assertEquals(1, Percurso.recomendarAlternativa(List.of(principal, alternativa), List.of(meio)));
        assertEquals(2, Percurso.recomendarAlternativa(List.of(principal, bloqueada, alternativa), List.of(meio)));
        assertEquals(-1, Percurso.recomendarAlternativa(List.of(principal, bloqueada), List.of(meio)));
        assertEquals(-1, Percurso.recomendarAlternativa(List.of(principal, alternativa), List.of()));
        assertEquals(-1, Percurso.recomendarAlternativa(List.of(principal, alternativa),
                List.of(meio, new Ponto(meio.longitude(), 30.0 / 111_195))));
    }

    private Rota candidata(String id, double deslocamento, int impedimentos) {
        double latitude = deslocamento / 111_195;
        return new Rota(id, List.of(new Ponto(origem.longitude(), latitude), new Ponto(fim.longitude(), latitude)),
                100, 90, "PARCIAL", .2, impedimentos, 0, "");
    }

    private Percurso percurso(boolean impeditiva) {
        var rota = new Rota("rota", List.of(origem, meio, fim), 100, 90, "PARCIAL", .2, 0, 1, "",
                List.of(new Etapa("Siga em frente", List.of(origem, meio)), new Etapa("Continue", List.of(meio, fim))),
                List.of(new Evidencia(.5, .7, "piso irregular", impeditiva)));
        return new Percurso(rota, "cadeirante", new LocalRota(fim, null));
    }

    @Test public void preservaInstrucaoComGpsImprecisoEConfirmaChegada() {
        var sessao = percurso();
        sessao.atualizar(origem, 4, 1000);
        sessao.atualizar(ponto(20), 4, 4000);
        assertEquals("Evidências próximas: piso irregular", sessao.alerta);
        sessao.alerta = "";
        sessao.atualizar(ponto(40), 4, 7000);
        assertEquals("", sessao.alerta);
        sessao.atualizar(fim, 80, 9000);
        assertEquals("Siga em frente", sessao.instrucao());
        assertFalse(sessao.chegou());
        sessao.atualizar(ponto(65), 4, 10000);
        sessao.atualizar(ponto(75), 4, 12000);
        assertEquals("Continue", sessao.instrucao());
        sessao.atualizar(fim, 4, 14000);
        sessao.atualizar(fim, 4, 16000);
        assertFalse(sessao.chegou());
        sessao.atualizar(fim, 4, 18000);
        assertTrue(sessao.chegou());
    }

    @Test public void viaParalelaNaoAvancaInstrucaoNemDisparaAlerta() {
        var sessao = percurso();
        sessao.atualizar(origem, 4, 1000);
        var viaParalela = new Ponto(ponto(25).longitude(), 18.0 / 111_195);
        sessao.atualizar(viaParalela, 4, 3000);
        sessao.atualizar(viaParalela, 4, 5000);
        assertEquals(100, sessao.restante(), .001);
        assertEquals("", sessao.alerta);
        assertFalse(sessao.desviou());
    }

    @Test public void replanejaUmaVezParaBloqueioAdianteComGpsPreciso() {
        var sessao = percurso(true);
        sessao.ativo = true;
        sessao.atualizar(ponto(20), 50, 1000);
        assertNull(sessao.consumirBloqueioProximo());
        sessao.atualizar(ponto(20), 4, 3000);
        assertNotNull(sessao.consumirBloqueioProximo());
        sessao.atualizar(ponto(25), 4, 5000);
        assertNull(sessao.consumirBloqueioProximo());

        var atencao = percurso();
        atencao.ativo = true;
        atencao.atualizar(ponto(20), 4, 1000);
        assertNull(atencao.consumirBloqueioProximo());
    }

    @Test public void eliminaAlternativaQueVoltaAoTrechoBloqueado() {
        var sessao = percurso(true);
        var bloqueio = sessao.rota.evidencias().get(0);
        var desvio = List.of(origem, new Ponto(meio.longitude(), .0002),
                new Ponto(ponto(80).longitude(), .0002), fim);
        assertFalse(sessao.evitaBloqueio(alternativa(List.of(origem, fim), 0), bloqueio));
        assertFalse(sessao.evitaBloqueio(alternativa(desvio, 1), bloqueio));
        assertTrue(sessao.evitaBloqueio(alternativa(desvio, 0), bloqueio));
    }

    private Rota alternativa(List<Ponto> pontos, int impeditivas) {
        return new Rota("alternativa", pontos, 110, 90, "PARCIAL", .2, impeditivas, 0, "");
    }

    @Test public void exigeDesvioPersistenteEInterrompeAoPausar() {
        var sessao = percurso();
        var fora = new Ponto(0, .001);
        sessao.atualizar(fora, 4, 1000);
        sessao.atualizar(fora, 4, 3000);
        assertFalse(sessao.desviou());
        sessao.atualizar(fora, 4, 5000);
        assertTrue(sessao.desviou());
        sessao.ativo = true;
        sessao.interromper();
        assertFalse(sessao.ativo);
        assertFalse(sessao.desviou());
        assertNull(sessao.posicao);
    }
}
