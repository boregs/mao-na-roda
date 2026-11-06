package com.maonaroda.mao_na_roda;

import android.Manifest;
import android.os.*;
import android.text.*;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.maps.SupportMapFragment;
import com.maonaroda.mao_na_roda.mapa.*;
import com.maonaroda.mao_na_roda.data.DadosRotas.*;
import com.maonaroda.mao_na_roda.data.ApiClient;
import com.maonaroda.mao_na_roda.data.ContaRepository;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.maonaroda.mao_na_roda.data.RotasRepository;
import com.maonaroda.mao_na_roda.data.SessaoStore;
import com.google.android.material.snackbar.Snackbar;
import com.maonaroda.mao_na_roda.localizacao.LocalizacaoController;
import java.util.List;

public class HomeFragment extends Fragment {
    private HomeViewModel model;
    private RenderizadorMapa mapa;
    private View root;
    private boolean atualizando;
    private List<Rota> ultimasRotas;
    private int ultimaSelecionada = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
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
        view.findViewById(R.id.account).setOnClickListener(v -> abrirConta());
        view.findViewById(R.id.report_barrier).setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_home_to_report_form));
        view.findViewById(R.id.report_history).setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_home_to_report_history));
        view.findViewById(R.id.about).setOnClickListener(v -> HowItWorksSheet.show(getParentFragmentManager()));
        model.estado().observe(getViewLifecycleOwner(), this::renderizar);
        if (BuildConfig.MAPS_CONFIGURED) {
            view.findViewById(R.id.map_status).setVisibility(View.GONE);
            var fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
            if (fragment == null) { fragment = SupportMapFragment.newInstance(); getChildFragmentManager().beginTransaction().replace(R.id.map_container, fragment).commitNow(); }
            fragment.getMapAsync(googleMap -> {
                if (root != view) return;
                mapa = new RenderizadorMapa(googleMap, requireContext()); ajustarMapa();
                renderizar(model.estado().getValue());
            });
        }
        model.carregarPerfis();
    }
    private void abrirConta() {
        var sessao = SessaoStore.de(requireContext());
        String token = sessao.tokenValido();
        String email = sessao.ultimoEmail();
        var dialogo = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.account_title)
                .setNegativeButton(R.string.close, null);
        if (token != null) {
            dialogo.setMessage(getString(R.string.account_signed_in, email))
                    .setPositiveButton(R.string.sign_out, (d, which) -> sair(token))
                    .setNeutralButton(R.string.delete_account, (d, which) -> confirmarExclusao(token)).show();
            return;
        }
        dialogo.setMessage(email == null ? getString(R.string.account_signed_out)
                        : getString(R.string.account_expired, email))
                .setPositiveButton(R.string.sign_in, (d, which) -> abrirAutenticacao(true));
        if (email == null) dialogo.setNeutralButton(R.string.create_account, (d, which) -> abrirAutenticacao(false));
        dialogo.show();
    }
    private void abrirAutenticacao(boolean entrar) {
        Bundle argumentos = new Bundle();
        argumentos.putBoolean("signIn", entrar);
        NavHostFragment.findNavController(this).navigate(R.id.authFragment, argumentos);
    }
    /** UC10: explica os efeitos (RN16) e pede a senha antes de excluir a conta. */
    private void confirmarExclusao(String token) {
        var contexto = requireContext();
        var campoLayout = new TextInputLayout(contexto);
        campoLayout.setHint(R.string.password);
        campoLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        var campo = new TextInputEditText(campoLayout.getContext());
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        campoLayout.addView(campo);
        int margem = Math.round(24 * getResources().getDisplayMetrics().density);
        var container = new FrameLayout(contexto);
        container.setPadding(margem, margem / 2, margem, 0);
        container.addView(campoLayout);
        AlertDialog dialogo = new MaterialAlertDialogBuilder(contexto)
                .setTitle(R.string.delete_account_title)
                .setMessage(R.string.delete_account_explanation)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_account_confirm, null)
                .create();
        dialogo.setOnShowListener(d -> {
            Button confirmar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
            confirmar.setOnClickListener(v -> {
                String senha = campo.getText() == null ? "" : campo.getText().toString();
                if (senha.isEmpty()) {
                    campoLayout.setError(getString(R.string.password_required));
                    return;
                }
                campoLayout.setError(null);
                confirmar.setEnabled(false);
                var api = new ApiClient(BuildConfig.API_BASE_URL, r -> new Handler(Looper.getMainLooper()).post(r), BuildConfig.DEBUG);
                api.excluirConta(token, senha, new RotasRepository.Retorno<>() {
                    @Override public void sucesso(Void valor) {
                        api.close();
                        dialogo.dismiss();
                        encerrarSessaoLocal(R.string.account_deleted);
                    }
                    @Override public void falha(String mensagem) {
                        api.close();
                        if (ContaRepository.SESSAO_ENCERRADA.equals(mensagem)) {
                            // A resposta de uma exclusão anterior pode ter se perdido.
                            dialogo.dismiss();
                            encerrarSessaoLocal(R.string.delete_session_ended);
                            return;
                        }
                        confirmar.setEnabled(true);
                        campoLayout.setError(mensagem);
                    }
                });
            });
        });
        dialogo.show();
    }
    private void encerrarSessaoLocal(int mensagem) {
        if (!isAdded()) return;
        SessaoStore.de(requireContext()).encerrar();
        if (root != null) Snackbar.make(root, mensagem, Snackbar.LENGTH_LONG).show();
    }
    /** Encerra localmente na hora; o logout no servidor é feito em seguida, sem bloquear a tela. */
    private void sair(String token) {
        SessaoStore.de(requireContext()).encerrar();
        var api = new ApiClient(BuildConfig.API_BASE_URL, r -> new Handler(Looper.getMainLooper()).post(r), BuildConfig.DEBUG);
        api.sair(token, new RotasRepository.Retorno<>() {
            @Override public void sucesso(Void valor) { api.close(); }
            @Override public void falha(String mensagem) { api.close(); }
        });
        if (root != null) Snackbar.make(root, R.string.signed_out, Snackbar.LENGTH_SHORT).show();
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
        if (estado.perfis().isEmpty()) { model.carregarPerfis(); return; }
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
            for (int i = 0; i < estado.rotas().size(); i++) {
                final int index = i; var rota = estado.rotas().get(i);
                var button = botao(getString(R.string.route_summary, i + 1, rota.distancia(), rota.duracao() / 60,
                        rota.cobertura() * 100, rota.impeditivas(), rota.atencao()));
                button.setOnClickListener(v -> { model.selecionar(index); detalhes(rota); }); results.addView(button);
            }
        }
        if (mapa != null) { ajustarMapa(); mapa.renderizar(estado.rotas(), estado.selecionada(), requireContext(), root.getWidth(), root.getHeight()); }
    }
    private MaterialButton botao(String texto) {
        var button = new MaterialButton(requireContext()); button.setText(texto); button.setMinHeight(dp(56)); button.setMaxLines(8); button.setTextSize(18);
        button.setLayoutParams(new LinearLayout.LayoutParams(-1, -2)); return button;
    }
    private void detalhes(Rota rota) {
        var text = new TextView(requireContext()); text.setTextSize(20); text.setPadding(dp(20), dp(12), dp(20), dp(12));
        text.setText(getString(R.string.route_details, rota.qualidade(), rota.cobertura() * 100, rota.detalhes()));
        var scroll = new ScrollView(requireContext()); scroll.addView(text);
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.route_details_title).setView(scroll).setPositiveButton(R.string.close, null).show();
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
    @Override public void onStop() { localizacao.cancelar(); handler.removeCallbacks(prazoLocalizacao); super.onStop(); }
    @Override public void onDestroyView() {
        root = null; mapa = null; ultimasRotas = null; ultimaSelecionada = -1;
        super.onDestroyView();
    }
}
