package com.maonaroda.mao_na_roda;

import android.Manifest;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.*;
import android.text.style.StyleSpan;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.maonaroda.mao_na_roda.mapa.*;
import com.maonaroda.mao_na_roda.data.DadosRotas.*;
import com.maonaroda.mao_na_roda.localizacao.LocalizacaoController;
import java.io.IOException;
import java.util.List;
import androidx.lifecycle.Lifecycle;
import androidx.core.content.ContextCompat;
import com.maonaroda.mao_na_roda.data.RotasRepository;

public class HomeFragment extends Fragment {
    private HomeViewModel model;
    private RenderizadorMapa mapa;
    private View root;
    private boolean atualizando;
    private List<Rota> ultimasRotas;
    private List<BarrierReport> barreirasLocais = List.of();
    private int ultimaSelecionada = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LocalizacaoController gpsPercurso = new LocalizacaoController();
    private RotasRepository.Cancelavel replanejamento;
    private int geracaoReplanejamento;
    private boolean buscandoRota;
    private androidx.appcompat.app.AlertDialog revisaoRota;
    private final Runnable semSinal = () -> {
        if (model.percurso == null || !model.percurso.ativo) return;
        model.percurso.invalidarLeitura();
        mostrarPercurso();
    };
    private TextView progresso, orientacao, alerta, limites;
    private MaterialButton pausa, centro, encerrar, novoPlano;
    private final ActivityResultLauncher<String[]> permissaoPercurso = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), granted -> {
                if (root == null || model.percurso == null) return;
                if (Boolean.TRUE.equals(granted.get(Manifest.permission.ACCESS_FINE_LOCATION))) acompanhar();
                else {
                    model.mensagem("O acompanhamento precisa de localização precisa. A rota permanece no mapa.");
                    mostrarPercurso();
                }
            });
    private final LocalizacaoController localizacao = new LocalizacaoController();
    private final Runnable prazoLocalizacao = () -> {
        localizacao.cancelar(); if (model != null) model.mensagem(getString(R.string.location_unavailable));
    };
    private final ActivityResultLauncher<String[]> permissao = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), granted -> {
                if (root == null) return;
                if (Boolean.TRUE.equals(granted.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(granted.get(Manifest.permission.ACCESS_COARSE_LOCATION))) obterLocalizacao();
                else model.mensagem(getString(R.string.location_denied));
            });

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        return inflater.inflate(R.layout.fragment_home, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, Bundle state) {
        root = view; model = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        AccessibilityUi.apply(view, AccessibilityProfileStore.preferences(requireContext()));
        var origem = (EditText) view.findViewById(R.id.origin_input);
        var destino = (EditText) view.findViewById(R.id.destination_input);
        origem.setText(model.origemTexto); destino.setText(model.destinoTexto);
        origem.addTextChangedListener(watcher(true)); destino.addTextChangedListener(watcher(false));
        var panel = BottomSheetBehavior.from(view.findViewById(R.id.map_panel));
        view.findViewById(R.id.map_panel).post(() -> {
            if (root != view) return;
            panel.setMaxHeight((int) (view.getHeight() * .75));
        });
        view.findViewById(R.id.panel_toggle).setOnClickListener(v -> panel.setState(
                panel.getState() == BottomSheetBehavior.STATE_EXPANDED ? BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_EXPANDED));
        panel.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View sheet, int value) {
                if (root != null) ((TextView) root.findViewById(R.id.panel_toggle)).setText(
                        value == BottomSheetBehavior.STATE_EXPANDED ? R.string.collapse_planning : R.string.expand_planning);
                ajustarMapa();
            }
            @Override public void onSlide(@NonNull View sheet, float offset) { ajustarMapa(); }
        });
        if (model.origemTexto.isEmpty()) panel.setState(BottomSheetBehavior.STATE_EXPANDED);
        view.findViewById(R.id.search).setOnClickListener(v -> { ocultarTeclado(); model.consultar(); });
        view.findViewById(R.id.cancel_query).setOnClickListener(v -> model.cancelar());
        view.findViewById(R.id.profile).setOnClickListener(v -> escolherPerfil());
        view.findViewById(R.id.use_location).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.use_location).setMessage(R.string.location_consent)
                .setNegativeButton(R.string.close, null).setPositiveButton(R.string.allow_location, (d, which) ->
                        permissao.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})).show());
        view.findViewById(R.id.interface_preferences).setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_home_to_accessibility_profile));
        view.findViewById(R.id.account).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_title).setMessage(R.string.auth_unavailable).setPositiveButton(R.string.close, null).show());
        view.findViewById(R.id.report_barrier).setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_home_to_report_form));
        view.findViewById(R.id.report_history).setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_home_to_report_history));
        view.findViewById(R.id.about).setOnClickListener(v -> HowItWorksSheet.show(getParentFragmentManager()));
        montarPercurso();
        if (model.percurso == null && state != null && state.getBoolean("tinhaPercurso"))
            model.mensagem("A sessão de percurso foi perdida. Planeje a rota novamente.");
        carregarBarreirasLocais();
        model.estado().observe(getViewLifecycleOwner(), this::renderizar);
        if (BuildConfig.MAPS_CONFIGURED) {
            view.findViewById(R.id.map_status).setVisibility(View.GONE);
            var fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
            if (fragment == null) { fragment = SupportMapFragment.newInstance(); getChildFragmentManager().beginTransaction().replace(R.id.map_container, fragment).commitNow(); }
            fragment.getMapAsync(googleMap -> {
                if (root != view) return;
                mapa = new RenderizadorMapa(googleMap, requireContext());
                mostrarMarcadoresDeBarreira();
                ajustarMapa();
                renderizar(model.estado().getValue());
                var entrada = NavHostFragment.findNavController(this).getCurrentBackStackEntry();
                double[] ponto = entrada == null ? null
                        : entrada.getSavedStateHandle().remove(ReportFormFragment.PONTO_SALVO);
                if (ponto != null) {
                    panel.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    mapa.mostrarPonto(new LatLng(ponto[0], ponto[1]));
                }
            });
        }
        model.carregarPerfis();
    }
    private void carregarBarreirasLocais() {
        try {
            barreirasLocais = BarrierReportStore.from(requireContext()).all().stream()
                    .filter(relato -> relato.ponto != null)
                    .collect(java.util.stream.Collectors.toList());
        } catch (IOException erro) {
            barreirasLocais = List.of();
            Toast.makeText(requireContext(), R.string.report_markers_unavailable, Toast.LENGTH_LONG).show();
        }
    }
    private void mostrarMarcadoresDeBarreira() {
        var marcadores = barreirasLocais.stream()
                .map(relato -> new MarkerOptions()
                        .position(new LatLng(relato.ponto.latitude(), relato.ponto.longitude()))
                        .title(getString(R.string.report_marker_title))
                        .snippet(relato.category))
                .collect(java.util.stream.Collectors.toList());
        mapa.barreirasLocais(marcadores);
    }

    private String avisoBarreirasLocais(Rota rota) {
        int quantidade = (int) barreirasLocais.stream()
                .filter(relato -> Percurso.pertoDaRota(relato.ponto, rota.pontos()))
                .count();
        return quantidade == 0 ? "" : getResources()
                .getQuantityString(R.plurals.route_local_reports, quantidade, quantidade);
    }

    private String resumoRota(Rota rota, int numero) {
        return getString(R.string.route_summary, numero, rota.distancia(), rota.duracao() / 60,
                rota.cobertura() * 100, rota.impeditivas(), rota.atencao());
    }

    private void adicionarAvisoBarreira(LinearLayout painel, String aviso) {
        if (aviso.isEmpty()) return;
        var text = new TextView(requireContext());
        text.setTextAppearance(R.style.MaoText_Footer);
        var mensagem = new SpannableString(getString(R.string.route_local_warning, aviso));
        mensagem.setSpan(new StyleSpan(Typeface.BOLD), 0, mensagem.toString().indexOf('\n'),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setText(mensagem);
        text.setPadding(dp(12), dp(8), dp(12), dp(8));
        var fundo = new GradientDrawable();
        fundo.setColor(ContextCompat.getColor(requireContext(), R.color.route_warning_surface));
        fundo.setCornerRadius(dp(12));
        text.setBackground(fundo);
        AccessibilityUi.apply(text, AccessibilityProfileStore.preferences(requireContext()));
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.route_warning_text));
        var params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(8);
        painel.addView(text, params);
    }

    private void adicionarCartaoRota(LinearLayout resultados, Rota rota, int index, int recomendada) {
        var card = (MaterialCardView) getLayoutInflater().inflate(R.layout.item_route_result, resultados, false);
        int numero = index + 1;
        ((TextView) card.findViewById(R.id.route_card_title)).setText(getString(R.string.route_card_title, numero));
        ((TextView) card.findViewById(R.id.route_card_metrics)).setText(
                getString(R.string.route_card_metrics, rota.distancia(), rota.duracao() / 60));
        ((TextView) card.findViewById(R.id.route_card_evidence)).setText(getString(R.string.route_card_evidence,
                rota.cobertura() * 100, rota.impeditivas(), rota.atencao()));
        var detalhes = (MaterialButton) card.findViewById(R.id.route_card_details);
        detalhes.setContentDescription(getString(R.string.route_open_details_numbered, numero));
        detalhes.setOnClickListener(v -> {
            model.selecionar(index);
            detalhes(rota);
        });
        AccessibilityUi.apply(card, AccessibilityProfileStore.preferences(requireContext()));
        if (index == recomendada) {
            var badge = (TextView) card.findViewById(R.id.route_card_badge);
            var fundo = new GradientDrawable();
            fundo.setCornerRadius(dp(12));
            fundo.setColor(ContextCompat.getColor(requireContext(), R.color.selected_tint));
            badge.setBackground(fundo);
            badge.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_selected_tint));
            badge.setVisibility(View.VISIBLE);
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.ocean));
            card.setStrokeWidth(Math.max(card.getStrokeWidth(), dp(2)));
        } else if (card.getStrokeWidth() == 0) {
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.home_surface_outline));
            card.setStrokeWidth(dp(1));
        }
        String aviso = avisoBarreirasLocais(rota);
        if (index == 0 && recomendada >= 0) {
            aviso += "\n" + getString(R.string.route_local_recommendation, recomendada + 1);
        }
        adicionarAvisoBarreira(card.findViewById(R.id.route_card_alerts), aviso);
        resultados.addView(card);
    }
    private TextWatcher watcher(boolean inicio) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!atualizando) { localizacao.cancelar(); handler.removeCallbacks(prazoLocalizacao); model.editar(inicio, s.toString()); }
            }
            @Override public void afterTextChanged(Editable e) { }
        };
    }
    private void escolherPerfil() {
        var estado = model.estado().getValue();
        if (estado.perfis().isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.select_route_profile)
                    .setMessage("O catálogo de perfis ainda não está disponível. Confira a conexão com a API e tente novamente.")
                    .setNegativeButton(R.string.close, null)
                    .setPositiveButton("Tentar novamente", (dialog, index) -> model.carregarPerfis())
                    .show();
            return;
        }
        String[] nomes = new String[estado.perfis().size()]; int selecionado = -1;
        for (int i = 0; i < nomes.length; i++) { nomes[i] = estado.perfis().get(i).nome(); if (estado.perfis().get(i).codigo().equals(estado.perfil())) selecionado = i; }
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.select_route_profile)
                .setSingleChoiceItems(nomes, selecionado, (dialog, index) -> { model.perfil(estado.perfis().get(index).codigo()); dialog.dismiss(); })
                .setNegativeButton(R.string.close, null).show();
    }
    private void renderizar(HomeViewModel.Estado estado) {
        if (root == null || estado == null) return;
        atualizando = true;
        var origem = (EditText) root.findViewById(R.id.origin_input);
        var destino = (EditText) root.findViewById(R.id.destination_input);
        if (!origem.getText().toString().equals(model.origemTexto)) origem.setText(model.origemTexto);
        if (!destino.getText().toString().equals(model.destinoTexto)) destino.setText(model.destinoTexto);
        atualizando = false;
        ((TextView) root.findViewById(R.id.planning_status)).setText(estado.mensagem());
        root.findViewById(R.id.planning_progress).setVisibility(estado.carregando() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.search).setEnabled(!estado.carregando());
        String perfil = getString(R.string.no_route_profile);
        for (var p : estado.perfis()) if (p.codigo().equals(estado.perfil())) perfil = p.nome();
        ((TextView) root.findViewById(R.id.profile_summary)).setText(perfil);
        if (ultimasRotas != estado.rotas() || ultimaSelecionada != estado.selecionada()) {
            ultimasRotas = estado.rotas(); ultimaSelecionada = estado.selecionada();
            var results = (LinearLayout) root.findViewById(R.id.route_results); results.removeAllViews();
            var pontosDeBarreira = barreirasLocais.stream().map(relato -> relato.ponto)
                    .collect(java.util.stream.Collectors.toList());
            int recomendada = Percurso.recomendarAlternativa(estado.rotas(), pontosDeBarreira);
            for (int i = 0; i < estado.rotas().size(); i++) {
                adicionarCartaoRota(results, estado.rotas().get(i), i, recomendada);
            }
        }
        if (mapa != null) {
            ajustarMapa();
            var sessao = model.percurso;
            mapa.renderizar(sessao == null ? estado.rotas() : List.of(sessao.rota),
                    sessao == null ? estado.selecionada() : 0, requireContext(), root.getWidth(), root.getHeight());
        }
        mostrarPercurso();
    }
    private MaterialButton botao(String texto) {
        var button = new MaterialButton(requireContext()); button.setText(texto); button.setMinHeight(dp(56)); button.setMaxLines(8); button.setTextSize(18);
        button.setLayoutParams(new LinearLayout.LayoutParams(-1, -2)); return button;
    }
    private void detalhes(Rota rota) {
        var conteudo = new LinearLayout(requireContext());
        conteudo.setOrientation(LinearLayout.VERTICAL);
        conteudo.setPadding(dp(20), dp(12), dp(20), dp(12));
        adicionarAvisoBarreira(conteudo, avisoBarreirasLocais(rota));
        var text = new TextView(requireContext()); text.setTextSize(20);
        text.setText(getString(R.string.route_details, rota.qualidade(), rota.cobertura() * 100, rota.detalhes())
                + "\n\nDurante o percurso, o app pode enviar sua posição para buscar um desvio de uma barreira impeditiva. Você confirma qualquer troca de rota.");
        conteudo.addView(text);
        var scroll = new ScrollView(requireContext()); scroll.addView(conteudo);
        var dialogo = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.route_details_title)
                .setView(scroll).setNegativeButton(R.string.close, null);
        if (model.percurso == null && !model.estado().getValue().carregando()) {
            dialogo.setPositiveButton("Iniciar percurso", (d, w) -> {
                model.percurso = new Percurso(rota, model.estado().getValue().perfil(), LocalRota.deTexto(model.destinoTexto));
                mostrarPercurso();
                solicitarAcompanhamento();
            });
        }
        dialogo.show();
    }
    private void montarPercurso() {
        var painel = (LinearLayout) root.findViewById(R.id.percurso);
        orientacao = textoPercurso(painel, true);
        progresso = textoPercurso(painel, false);
        alerta = textoPercurso(painel, true);
        pausa = botao("Pausar percurso");
        centro = botao("Recentralizar na minha posição");
        novoPlano = botao("Buscar outra rota");
        encerrar = botao("Encerrar percurso");
        for (var controle : List.of(pausa, centro, novoPlano, encerrar)) painel.addView(controle);
        limites = textoPercurso(painel, false);
        AccessibilityUi.apply(painel, AccessibilityProfileStore.preferences(requireContext()));
        pausa.setOnClickListener(v -> {
            if (model.percurso.ativo) pausarPercurso();
            else solicitarAcompanhamento();
        });
        centro.setOnClickListener(v -> { if (mapa != null) mapa.recentralizar(); });
        novoPlano.setOnClickListener(v -> replanejar(null));
        encerrar.setOnClickListener(v -> {
            pausarPercurso();
            model.percurso = null;
            model.mensagem("Percurso encerrado.");
        });
    }

    private TextView textoPercurso(LinearLayout painel, boolean anunciar) {
        var texto = new TextView(requireContext());
        texto.setTextSize(18);
        if (anunciar) texto.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        painel.addView(texto, new LinearLayout.LayoutParams(-1, -2));
        return texto;
    }

    private void solicitarAcompanhamento() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) acompanhar();
        else new MaterialAlertDialogBuilder(requireContext()).setTitle("Localização durante o percurso")
                .setMessage("Usamos sua posição enquanto o percurso estiver ativo e esta tela estiver aberta. Perto de uma barreira impeditiva, a posição pode ser enviada para buscar um desvio.")
                .setNegativeButton(R.string.close, null).setPositiveButton(R.string.allow_location, (d, w) ->
                        permissaoPercurso.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION})).show();
    }

    private void acompanhar() {
        var sessao = model.percurso;
        if (root == null || sessao == null || sessao.ativo || sessao.chegou() || !getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;
        localizacao.cancelar();
        handler.removeCallbacks(prazoLocalizacao);
        sessao.ativo = gpsPercurso.acompanhar(requireContext(), local -> {
            if (model.percurso != sessao || !sessao.ativo) return;
            long momento = local.getElapsedRealtimeNanos() / 1_000_000;
            if (SystemClock.elapsedRealtime() - momento > 8000) return;
            handler.removeCallbacks(semSinal);
            handler.postDelayed(semSinal, 10000);
            sessao.atualizar(new Ponto(local.getLongitude(), local.getLatitude()),
                    local.hasAccuracy() ? local.getAccuracy() : 0, momento);
            if (sessao.chegou()) {
                gpsPercurso.cancelar();
                handler.removeCallbacks(semSinal);
                sessao.ativo = false;
                sessao.posicao = null;
                cancelarReplanejamento();
            } else if (!buscandoRota && (revisaoRota == null || !revisaoRota.isShowing())) {
                var bloqueio = sessao.consumirBloqueioProximo();
                if (bloqueio != null) replanejar(bloqueio);
            }
            mostrarPercurso();
        }, () -> {
            pausarPercurso();
            model.mensagem("Localização indisponível. Ative o GPS para retomar.");
        });
        if (sessao.ativo) handler.postDelayed(semSinal, 10000);
        model.mensagem(sessao.ativo ? "" : "Localização indisponível. Ative o GPS e confira a permissão.");
    }

    private void pausarPercurso() {
        cancelarReplanejamento();
        gpsPercurso.cancelar();
        handler.removeCallbacks(semSinal);
        if (model != null && model.percurso != null && !model.percurso.chegou()) {
            model.percurso.interromper();
            mostrarPercurso();
        }
    }

    private void mostrarPercurso() {
        if (root == null || progresso == null) return;
        var sessao = model.percurso;
        root.findViewById(R.id.percurso).setVisibility(sessao == null ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.route_results).setVisibility(sessao == null ? View.VISIBLE : View.GONE);
        for (int id : new int[]{R.id.origin_input, R.id.destination_input, R.id.profile, R.id.use_location})
            root.findViewById(id).setEnabled(sessao == null);
        root.findViewById(R.id.search).setEnabled(sessao == null && !model.estado().getValue().carregando());
        if (mapa != null) mapa.posicao(sessao == null ? null : sessao.posicao);
        if (sessao == null) return;
        String instrucao = sessao.chegou() ? "Você chegou ao destino." : sessao.instrucao();
        String proxima = sessao.proximaInstrucao();
        if (sessao.ativo && !proxima.isBlank()) instrucao += "\nEm seguida: " + proxima;
        if (!instrucao.contentEquals(orientacao.getText())) orientacao.setText(instrucao);
        String estado = sessao.chegou() ? "Percurso concluído"
                : !sessao.ativo ? "Percurso pausado"
                : sessao.posicao == null ? "Aguardando localização precisa"
                : sessao.desviou() ? "Possível saída da rota" : "Percurso em andamento";
        progresso.setText(String.format(java.util.Locale.getDefault(), "%s · Restam cerca de %.0f m", estado, sessao.restante()));
        String resumo = getString(R.string.route_details, sessao.rota.qualidade(), sessao.rota.cobertura() * 100,
                sessao.rota.detalhes());
        if (!resumo.contentEquals(limites.getText())) limites.setText(resumo);
        if (!sessao.alerta.contentEquals(alerta.getText())) alerta.setText(sessao.alerta);
        pausa.setVisibility(sessao.chegou() ? View.GONE : View.VISIBLE);
        pausa.setText(sessao.ativo ? "Pausar percurso" : "Retomar percurso");
        centro.setEnabled(mapa != null && sessao.posicao != null);
        novoPlano.setVisibility(sessao.desviou() ? View.VISIBLE : View.GONE);
        novoPlano.setEnabled(!buscandoRota);
        encerrar.setText(sessao.chegou() ? "Concluir percurso" : "Encerrar percurso");
    }

    private void cancelarReplanejamento() {
        geracaoReplanejamento++;
        buscandoRota = false;
        if (revisaoRota != null) revisaoRota.dismiss();
        revisaoRota = null;
        if (replanejamento != null) replanejamento.cancelar();
        replanejamento = null;
    }

    private void replanejar(Evidencia bloqueio) {
        var sessao = model.percurso;
        if (sessao == null || !sessao.ativo || sessao.posicao == null || buscandoRota) return;
        var origem = sessao.posicao;
        double distanciaRestante = sessao.restante();
        int geracao = ++geracaoReplanejamento;
        buscandoRota = true;
        model.mensagem(bloqueio == null ? "Buscando outra rota…" : "Buscando desvio para: " + bloqueio.descricao());
        var chamada = model.replanejar(sessao, origem, new RotasRepository.Retorno<>() {
            private boolean concluirConsulta() {
                if (geracao != geracaoReplanejamento) return false;
                buscandoRota = false;
                replanejamento = null;
                return root != null && model.percurso == sessao && sessao.ativo;
            }

            @Override public void sucesso(Consulta value) {
                if (!concluirConsulta()) return;
                if (bloqueio != null && !sessao.bloqueioAindaAdiante(bloqueio)) {
                    model.mensagem("A sugestão expirou. O percurso atual foi mantido.");
                    return;
                }
                var alternativas = bloqueio == null ? value.rotas()
                        : value.rotas().stream().filter(rota -> sessao.evitaBloqueio(rota, bloqueio))
                                .collect(java.util.stream.Collectors.toList());
                if (alternativas.isEmpty()) {
                    model.mensagem("Nenhuma alternativa adequada encontrada. O percurso atual foi mantido.");
                    return;
                }
                model.mensagem("Revise as alternativas antes de trocar de rota.");
                var opcoes = new String[alternativas.size()];
                for (int i = 0; i < opcoes.length; i++) {
                    var rota = alternativas.get(i);
                    opcoes[i] = resumoRota(rota, i + 1);
                    String aviso = avisoBarreirasLocais(rota);
                    if (!aviso.isEmpty()) opcoes[i] += "\n" + getString(R.string.route_local_warning, aviso);
                    if (bloqueio != null) {
                        opcoes[i] += String.format(java.util.Locale.getDefault(), "\nDiferença estimada: %+.0f m",
                                rota.distancia() - distanciaRestante);
                    }
                }
                String titulo = bloqueio == null ? "Revise as novas rotas" : "Desvio sugerido para: " + bloqueio.descricao();
                revisaoRota = new MaterialAlertDialogBuilder(requireContext()).setTitle(titulo)
                        .setItems(opcoes, (d, i) -> confirmarRota(sessao, alternativas.get(i), bloqueio))
                        .setNegativeButton(R.string.close, null).show();
            }
            @Override public void falha(String erro) {
                if (concluirConsulta()) model.mensagem(erro + " O percurso atual foi mantido.");
            }
        });
        if (buscandoRota && geracao == geracaoReplanejamento) replanejamento = chamada;
    }

    private void confirmarRota(Percurso anterior, Rota rota, Evidencia bloqueio) {
        var texto = new TextView(requireContext());
        texto.setTextSize(18);
        texto.setPadding(dp(20), dp(12), dp(20), dp(12));
        texto.setText(getString(R.string.route_details, rota.qualidade(), rota.cobertura() * 100, rota.detalhes()));
        var scroll = new ScrollView(requireContext());
        scroll.addView(texto);
        AccessibilityUi.apply(scroll, AccessibilityProfileStore.preferences(requireContext()));
        revisaoRota = new MaterialAlertDialogBuilder(requireContext()).setTitle("Confirmar nova rota").setView(scroll)
                .setNegativeButton(R.string.close, null).setPositiveButton("Usar esta rota", (d, w) -> {
                    if (model.percurso != anterior || !anterior.ativo || anterior.chegou()) return;
                    if (bloqueio != null && !anterior.bloqueioAindaAdiante(bloqueio)) {
                        model.mensagem("A sugestão expirou. O percurso atual foi mantido.");
                        return;
                    }
                    pausarPercurso();
                    model.percurso = new Percurso(rota, anterior.perfil, anterior.destino);
                    model.substituirRota(rota);
                    acompanhar();
                }).show();
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }
    private void ajustarMapa() {
        if (mapa != null && root != null) mapa.padding(Math.max(dp(12), root.getHeight() - (int) root.findViewById(R.id.map_panel).getY()));
    }
    private void ocultarTeclado() {
        if (root != null) ((android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(root.getWindowToken(), 0);
    }
    private void obterLocalizacao() {
        model.mensagem(getString(R.string.locating)); handler.removeCallbacks(prazoLocalizacao); handler.postDelayed(prazoLocalizacao, 5000);
        localizacao.obter(requireContext(), ponto -> { handler.removeCallbacks(prazoLocalizacao); if (root != null) model.localizar(ponto); },
                () -> { handler.removeCallbacks(prazoLocalizacao); if (root != null) model.mensagem(getString(R.string.location_unavailable)); });
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("tinhaPercurso", model != null && model.percurso != null);
    }

    @Override public void onStop() {
        localizacao.cancelar();
        handler.removeCallbacks(prazoLocalizacao);
        pausarPercurso();
        super.onStop();
    }
    @Override public void onDestroyView() {
        root = null; mapa = null; ultimasRotas = null; ultimaSelecionada = -1;
        super.onDestroyView();
    }
}
