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
        LocalRota origem, destino;
        int canceladas;
        @Override public Cancelavel perfis(Retorno<List<Perfil>> retorno) {
            retorno.sucesso(List.of(new Perfil("cadeirante", "Cadeirante"))); return () -> { };
        }
        @Override public Cancelavel planejar(LocalRota a, LocalRota b, String perfil, Retorno<Consulta> retorno) {
            origem = a; destino = b; consultas.add(retorno); return () -> canceladas++;
        }
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
    @Test public void enviaEnderecoSemBuscaSeparadaEPreservaDestinoPorCoordenadas() {
        model.editar(true, " Estação Carrão, São Paulo "); model.consultar();
        assertEquals("Estação Carrão, São Paulo", repository.origem.endereco());
        assertNull(repository.origem.coordenada());
        assertEquals(new Ponto(-46.7, -23.6), repository.destino.coordenada());
        assertEquals(1, repository.consultas.size());
    }
    @Test public void coordenadasInvalidasNaoSaoEnviadasComoEndereco() {
        for (String invalida : List.of("91, -46", "NaN, 0", "Infinity, 0", "1e3, -46")) {
            model.editar(true, invalida); model.consultar();
            assertTrue(model.estado().getValue().mensagem().contains("fora dos limites"));
        }
        assertTrue(repository.consultas.isEmpty());
    }
    @Test public void editarLocalDescartaRotaEIgnoraRespostaAntiga() {
        model.consultar(); repository.consultas.get(0).sucesso(resposta("anterior"));
        model.consultar(); model.editar(false, "Av. Paulista, São Paulo");
        repository.consultas.get(1).sucesso(resposta("tardia"));
        assertTrue(model.estado().getValue().rotas().isEmpty());
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
