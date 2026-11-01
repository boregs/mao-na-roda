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
            List<Rota> rotas, int selecionada, List<Destino> destinos, boolean buscaOrigem) { }
    private final RotasRepository repository;
    private final MutableLiveData<Estado> estado = new MutableLiveData<>();
    private List<Perfil> perfis = List.of();
    private List<Rota> rotas = List.of();
    private List<Destino> destinos = List.of();
    private String perfil = "", mensagem = "Informe origem e destino para começar.";
    private boolean carregando, buscaOrigem, carregandoCatalogo;
    private int selecionada;
    private long geracaoRota, geracaoBusca;
    private RotasRepository.Cancelavel consulta, busca, catalogo;
    public String origemTexto = "", destinoTexto = "";
    private Ponto origem, destino;

    public HomeViewModel() {
        this(new ApiClient(BuildConfig.API_BASE_URL, r -> new Handler(Looper.getMainLooper()).post(r)));
    }
    public HomeViewModel(RotasRepository repository) { this.repository = repository; publicar(); }
    public LiveData<Estado> estado() { return estado; }
    private void publicar() { estado.setValue(new Estado(carregando, mensagem, perfis, perfil, rotas, selecionada, destinos, buscaOrigem)); }
    public void carregarPerfis() {
        if (!perfis.isEmpty() || carregandoCatalogo) return;
        carregandoCatalogo = true;
        catalogo = repository.perfis(new RotasRepository.Retorno<>() {
            @Override public void sucesso(List<Perfil> value) {
                perfis = value; catalogo = null; carregandoCatalogo = false;
                mensagem = value.isEmpty() ? "Nenhum perfil disponível no servidor." : "Escolha o perfil e informe os locais.";
                publicar();
            }
            @Override public void falha(String erro) { catalogo = null; carregandoCatalogo = false; mensagem = erro; publicar(); }
        });
    }
    private void invalidar() {
        geracaoRota++; geracaoBusca++;
        if (consulta != null) consulta.cancelar(); if (busca != null) busca.cancelar();
        carregando = false; rotas = List.of(); destinos = List.of(); selecionada = 0;
    }
    public void editar(boolean inicio, String texto) {
        if (texto.equals(inicio ? origemTexto : destinoTexto)) return;
        invalidar();
        if (inicio) { origemTexto = texto; origem = null; } else { destinoTexto = texto; destino = null; }
        mensagem = "Consulte a rota após selecionar os locais."; publicar();
    }
    public void perfil(String codigo) {
        if (perfis.stream().noneMatch(p -> p.codigo().equals(codigo))) return;
        if (!perfil.equals(codigo)) invalidar();
        perfil = codigo; mensagem = "Perfil selecionado para esta sessão."; publicar();
    }
    public void buscar(boolean inicio) {
        String texto = inicio ? origemTexto : destinoTexto;
        if (texto.trim().length() < 3) { mensagem = "Digite pelo menos três caracteres ou latitude, longitude."; publicar(); return; }
        geracaoBusca++; long id = geracaoBusca;
        if (busca != null) busca.cancelar();
        buscaOrigem = inicio; destinos = List.of(); mensagem = "Buscando locais…"; publicar();
        busca = repository.buscar(texto, new RotasRepository.Retorno<>() {
            @Override public void sucesso(List<Destino> value) {
                if (id != geracaoBusca) return;
                destinos = value; mensagem = value.isEmpty() ? "Nenhum local encontrado. Tente um endereço mais completo." : "Selecione um local para " + (inicio ? "origem." : "destino."); publicar();
            }
            @Override public void falha(String erro) { if (id == geracaoBusca) { mensagem = erro; publicar(); } }
        });
    }
    public void escolher(Destino local) {
        boolean inicio = buscaOrigem; invalidar();
        if (inicio) { origem = local.coordenada(); origemTexto = local.nome(); }
        else { destino = local.coordenada(); destinoTexto = local.nome(); }
        mensagem = "Local selecionado. Você pode consultar a rota."; publicar();
    }
    public void localizar(Ponto ponto) {
        invalidar(); origem = ponto;
        origemTexto = ponto.latitude() + ", " + ponto.longitude();
        mensagem = "Origem obtida uma vez. Confira antes de consultar."; publicar();
    }
    public void mensagem(String texto) { mensagem = texto; publicar(); }
    public void consultar() {
        Ponto de = origem == null ? Ponto.manual(origemTexto) : origem;
        Ponto para = destino == null ? Ponto.manual(destinoTexto) : destino;
        if (perfil.isEmpty()) { mensagem = "Selecione um perfil de mobilidade."; publicar(); return; }
        if (de == null || para == null) { mensagem = "Selecione os locais encontrados ou informe latitude, longitude."; publicar(); return; }
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
        geracaoRota++; geracaoBusca++;
        if (consulta != null) consulta.cancelar(); if (busca != null) busca.cancelar();
        carregando = false; destinos = List.of(); mensagem = "Consulta cancelada."; publicar();
    }
    @Override protected void onCleared() { repository.close(); }
}
