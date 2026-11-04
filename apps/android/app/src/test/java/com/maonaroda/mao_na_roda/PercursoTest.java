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

    private Percurso percurso() {
        var rota = new Rota("rota", List.of(origem, meio, fim), 100, 90, "PARCIAL", .2, 0, 1, "",
                List.of(new Etapa("Siga em frente", List.of(origem, meio)), new Etapa("Continue", List.of(meio, fim))),
                List.of(new Evidencia(.5, .7, "piso irregular")));
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
