package com.maonaroda.mao_na_roda;

import com.maonaroda.mao_na_roda.data.ApiClient;
import org.junit.Test;
import static org.junit.Assert.*;

public class RotasJsonTest {
    @Test public void preservaOrdemGeoJsonEQualidadeDesconhecida() throws Exception {
        var consulta = ApiClient.lerConsulta("""
            {"resultado":"PARCIAL","rotas":[{"id":"rota-1","geometria":{"type":"LineString","coordinates":[[-46.6,-23.5],[-46.7,-23.6]]},
             "distanciaMetros":150,"duracaoSegundos":120,"qualidade":{"estado":"DESCONHECIDA","cobertura":0},
             "barreiras":{"impeditivas":0,"atencao":0},"trechos":[]}]}
            """);
        assertEquals(-46.6, consulta.rotas().get(0).pontos().get(0).longitude(), 0);
        assertEquals("DESCONHECIDA", consulta.rotas().get(0).qualidade());
        assertTrue(consulta.rotas().get(0).detalhes().contains("não garante"));
    }
    @Test public void respostaSemCandidataNaoInventaRota() throws Exception {
        assertTrue(ApiClient.lerConsulta("{\"resultado\":\"SEM_CANDIDATA\",\"rotas\":[]}").rotas().isEmpty());
    }
    @Test public void exibeFonteEMotivoDaDataDesconhecida() throws Exception {
        var consulta = ApiClient.lerConsulta("""
            {"resultado":"PARCIAL","rotas":[{"id":"rota-1","geometria":{"type":"LineString","coordinates":[[-46.6,-23.5],[-46.7,-23.6]]},
             "distanciaMetros":150,"duracaoSegundos":120,"qualidade":{"estado":"DESCONHECIDA","cobertura":0},
             "barreiras":{"impeditivas":1,"atencao":0},"trechos":[{"barreiras":[{"tipo":"escada_sem_alternativa","classificacao":"impeditiva",
             "condicao":"presente","fonte":"openstreetmap","verificadoEm":null,"desatualizada":true,"referenciaFonte":"way/1",
             "motivoSemVerificacao":"Fonte sem data de verificação"}]}]}]}
            """);
        var detalhes = consulta.rotas().get(0).detalhes();
        assertTrue(detalhes.contains("OpenStreetMap")); assertTrue(detalhes.contains("data desconhecida"));
        assertTrue(detalhes.contains("Fonte sem data")); assertTrue(detalhes.contains("way/1"));
    }
}
