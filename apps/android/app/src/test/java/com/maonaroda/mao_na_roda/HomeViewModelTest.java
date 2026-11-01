package com.maonaroda.mao_na_roda;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.maonaroda.mao_na_roda.data.*;
import com.maonaroda.mao_na_roda.data.DadosRotas.*;
import com.maonaroda.mao_na_roda.mapa.HomeViewModel;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class HomeViewModelTest {
    @Rule public InstantTaskExecutorRule tarefas = new InstantTaskExecutorRule();
    private static final class FakeRepository implements RotasRepository {
        final List<Retorno<Consulta>> consultas = new ArrayList<>();
        final List<Retorno<List<Destino>>> buscas = new ArrayList<>();
        int canceladas;
        @Override public Cancelavel perfis(Retorno<List<Perfil>> retorno) {
            retorno.sucesso(List.of(new Perfil("cadeirante", "Cadeirante"))); return () -> { };
        }
        @Override public Cancelavel buscar(String texto, Retorno<List<Destino>> retorno) { buscas.add(retorno); return () -> canceladas++; }
        @Override public Cancelavel planejar(Ponto a, Ponto b, String perfil, Retorno<Consulta> retorno) { consultas.add(retorno); return () -> canceladas++; }
        @Override public void close() { }
    }
    private FakeRepository repository;
    private HomeViewModel model;
    @Before public void preparar() {
        repository = new FakeRepository(); model = new HomeViewModel(repository);
        model.carregarPerfis(); model.perfil("cadeirante");
        model.editar(true, "-23.5, -46.6"); model.editar(false, "-23.6, -46.7");
    }
    private Consulta resposta(String id) {
        return new Consulta("PARCIAL", List.of(new Rota(id, List.of(new Ponto(-46.6, -23.5), new Ponto(-46.7, -23.6)),
                100, 120, "DESCONHECIDA", 0, 0, 0, "Dados desconhecidos")), "Sugestão informativa");
    }
    @Test public void respostaAntigaNaoSubstituiConsultaAtual() {
        model.consultar(); model.consultar();
        repository.consultas.get(1).sucesso(resposta("atual"));
        repository.consultas.get(0).sucesso(resposta("antiga"));
        assertEquals("atual", model.estado().getValue().rotas().get(0).id());
        assertTrue(repository.canceladas > 0);
    }
    @Test public void cancelamentoDescartaRespostaTardia() {
        model.consultar(); model.cancelar(); repository.consultas.get(0).sucesso(resposta("tardia"));
        assertTrue(model.estado().getValue().rotas().isEmpty()); assertFalse(model.estado().getValue().carregando());
    }
    @Test public void erroEmNovaTentativaPreservaRotaAnterior() {
        model.consultar(); repository.consultas.get(0).sucesso(resposta("anterior"));
        model.consultar(); repository.consultas.get(1).falha("Sem conexão");
        assertEquals("anterior", model.estado().getValue().rotas().get(0).id());
        assertEquals("Sem conexão", model.estado().getValue().mensagem());
    }
    @Test public void buscaAntigaNaoTrocaResultadosDoOutroCampo() {
        model.editar(true, "Estação Carrão"); model.buscar(true);
        model.editar(false, "UNICID"); model.buscar(false);
        repository.buscas.get(0).sucesso(List.of(new Destino("antigo", "Antigo", new Ponto(-46, -23))));
        assertTrue(model.estado().getValue().destinos().isEmpty());
        repository.buscas.get(1).sucesso(List.of(new Destino("novo", "Novo", new Ponto(-46, -23))));
        assertEquals("novo", model.estado().getValue().destinos().get(0).id());
    }
    @Test public void sessaoNovaNaoRestauraPerfilOuRota() {
        var nova = new HomeViewModel(new FakeRepository());
        assertTrue(nova.estado().getValue().perfil().isEmpty()); assertTrue(nova.estado().getValue().rotas().isEmpty());
        assertEquals("", nova.origemTexto);
    }
    @Test public void coordenadasManuaisSaoInvertidasSomenteNaEntrada() {
        var ponto = Ponto.manual("-23.5, -46.6");
        assertEquals(-46.6, ponto.longitude(), 0); assertEquals(-23.5, ponto.latitude(), 0);
        assertNull(Ponto.manual("rua exemplo")); assertNull(Ponto.manual("91, -46"));
    }
}
