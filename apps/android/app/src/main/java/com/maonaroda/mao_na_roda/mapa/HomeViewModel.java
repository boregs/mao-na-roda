package com.maonaroda.mao_na_roda.mapa;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.maonaroda.mao_na_roda.BuildConfig;
import com.maonaroda.mao_na_roda.data.*;
import com.maonaroda.mao_na_roda.data.DadosRotas.*;
import java.util.List;

public class HomeViewModel extends ViewModel {
    public record Estado(boolean carregando, String mensagem, List<Perfil> perfis, String perfil,
            List<Rota> rotas, int selecionada) { }
    private final RotasRepository repository;
    private final MutableLiveData<Estado> estado = new MutableLiveData<>();
    private List<Perfil> perfis = List.of();
    private List<Rota> rotas = List.of();
    private String perfil = "", mensagem = "Informe origem e destino para começar.";
    private boolean carregando, carregandoCatalogo;
    private int selecionada;
    private long geracaoRota;
    private RotasRepository.Cancelavel consulta, catalogo;
    public Percurso percurso;
    public RotasRepository.Cancelavel replanejar(Percurso sessao, Ponto origem, RotasRepository.Retorno<Consulta> retorno) {
        return repository.planejar(new LocalRota(origem, null), sessao.destino, sessao.perfil, retorno);
    }

    public void substituirRota(Rota rota) {
        invalidar();
        rotas = List.of(rota);
        publicar();
    }

    public String origemTexto = "", destinoTexto = "";

    public HomeViewModel() {
        this(new ApiClient(BuildConfig.API_BASE_URL, r -> new Handler(Looper.getMainLooper()).post(r), BuildConfig.DEBUG));
    }
    public HomeViewModel(RotasRepository repository) { this.repository = repository; publicar(); }
    public LiveData<Estado> estado() { return estado; }
    private void publicar() { estado.setValue(new Estado(carregando, mensagem, perfis, perfil, rotas, selecionada)); }
    public void carregarPerfis() {
        if (!perfis.isEmpty() || carregandoCatalogo) return;
        carregandoCatalogo = true;
        catalogo = repository.perfis(new RotasRepository.Retorno<>() {
            @Override public void sucesso(List<Perfil> value) {
                perfis = value; catalogo = null; carregandoCatalogo = false;
                mensagem = value.isEmpty() ? "Nenhum perfil disponível no servidor." : "Escolha o perfil e informe os endereços.";
                publicar();
            }
            @Override public void falha(String erro) { catalogo = null; carregandoCatalogo = false; mensagem = erro; publicar(); }
        });
    }
    private void invalidar() {
        geracaoRota++;
        if (consulta != null) consulta.cancelar();
        carregando = false; rotas = List.of(); selecionada = 0;
    }
    public void editar(boolean inicio, String texto) {
        if (texto.equals(inicio ? origemTexto : destinoTexto)) return;
        invalidar();
        if (inicio) origemTexto = texto; else destinoTexto = texto;
        mensagem = "Consulte a rota após informar origem e destino."; publicar();
    }
    public void perfil(String codigo) {
        if (perfis.stream().noneMatch(p -> p.codigo().equals(codigo))) return;
        if (!perfil.equals(codigo)) invalidar();
        perfil = codigo; mensagem = "Perfil selecionado para esta sessão."; publicar();
    }
    public void localizar(Ponto ponto) {
        invalidar(); origemTexto = ponto.latitude() + ", " + ponto.longitude();
        mensagem = "Origem obtida uma vez. Confira antes de consultar."; publicar();
    }
    public void mensagem(String texto) { mensagem = texto; publicar(); }
    public void consultar() {
        if (perfil.isEmpty()) { mensagem = "Selecione um perfil de mobilidade."; publicar(); return; }
        LocalRota de, para;
        try { de = LocalRota.deTexto(origemTexto); para = LocalRota.deTexto(destinoTexto); }
        catch (IllegalArgumentException e) { mensagem = e.getMessage(); publicar(); return; }
        if (de.equals(para)) { mensagem = "Origem e destino devem ser diferentes."; publicar(); return; }
        long id = ++geracaoRota; if (consulta != null) consulta.cancelar();
        carregando = true; mensagem = "Consultando rotas e evidências…"; publicar();
        consulta = repository.planejar(de, para, perfil, new RotasRepository.Retorno<>() {
            @Override public void sucesso(Consulta value) {
                if (id != geracaoRota) return;
                carregando = false; rotas = value.rotas(); selecionada = 0;
                mensagem = rotas.isEmpty() ? "Nenhuma rota candidata encontrada para estes locais." : value.aviso(); publicar();
            }
            @Override public void falha(String erro) { if (id == geracaoRota) { carregando = false; mensagem = erro; publicar(); } }
        });
    }
    public void selecionar(int index) { if (index >= 0 && index < rotas.size()) { selecionada = index; publicar(); } }
    public void cancelar() {
        geracaoRota++; if (consulta != null) consulta.cancelar();
        carregando = false; mensagem = "Consulta cancelada."; publicar();
    }
    @Override protected void onCleared() { repository.close(); }
}
