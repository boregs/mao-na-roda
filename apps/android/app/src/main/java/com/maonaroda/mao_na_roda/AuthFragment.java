package com.maonaroda.mao_na_roda;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.maonaroda.mao_na_roda.data.ApiClient;
import com.maonaroda.mao_na_roda.data.ContaRepository.Sessao;
import com.maonaroda.mao_na_roda.data.RotasRepository.Cancelavel;
import com.maonaroda.mao_na_roda.data.RotasRepository.Retorno;
import com.maonaroda.mao_na_roda.data.SessaoStore;

public class AuthFragment extends Fragment {
    boolean signIn;
    EditText name, surname, email, password;
    TextView error;
    Button submit;
    private ApiClient api;
    private Cancelavel chamada;

    public View onCreateView(LayoutInflater i, ViewGroup p, Bundle b) {
        return i.inflate(R.layout.fragment_auth, p, false);
    }

    public void onViewCreated(@NonNull View v, Bundle b) {
        signIn = getArguments() != null && getArguments().getBoolean("signIn");
        api = new ApiClient(BuildConfig.API_BASE_URL, r -> new Handler(Looper.getMainLooper()).post(r), BuildConfig.DEBUG);
        name = v.findViewById(R.id.name);
        surname = v.findViewById(R.id.surname);
        email = v.findViewById(R.id.email);
        password = v.findViewById(R.id.password);
        error = v.findViewById(R.id.error);
        submit = v.findViewById(R.id.submit);
        com.google.android.material.appbar.MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setTitle(signIn ? R.string.sign_in : R.string.create_account);
        toolbar.setNavigationOnClickListener(
                x -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        v.<TextView>findViewById(R.id.title).setText(
                signIn ? R.string.auth_signin_title : R.string.auth_create_title);
        v.<TextView>findViewById(R.id.body).setText(
                signIn ? R.string.auth_signin_body : R.string.auth_create_body);
        name.setVisibility(signIn ? View.GONE : View.VISIBLE);
        surname.setVisibility(signIn ? View.GONE : View.VISIBLE);
        submit.setText(signIn ? R.string.sign_in : R.string.create_account);
        v.<TextView>findViewById(R.id.switchMode).setText(
                signIn ? R.string.create_account : R.string.sign_in_link);
        // UC07 A3: após a expiração, a última conta aparece só como identificação.
        String ultimo = SessaoStore.de(requireContext()).ultimoEmail();
        if (signIn && ultimo != null && email.getText().length() == 0) email.setText(ultimo);
        submit.setOnClickListener(x -> submit());
        v.findViewById(R.id.guest).setOnClickListener(x -> irParaInicio());
        v.findViewById(R.id.switchMode).setOnClickListener(x -> {
            Bundle a = new Bundle();
            a.putBoolean("signIn", !signIn);
            NavHostFragment.findNavController(this).navigate(R.id.authFragment, a);
        });
    }

    @Override public void onDestroyView() {
        if (chamada != null) chamada.cancelar();
        chamada = null;
        if (api != null) api.close();
        api = null;
        super.onDestroyView();
    }

    /** Vai para o mapa: a partir do onboarding (fluxo original) ou de volta ao mapa, se veio dele. */
    private void irParaInicio() {
        requireContext().getSharedPreferences("prefs", 0)
                .edit()
                .putBoolean("onboarding_completed", true)
                .apply();
        NavController nav = NavHostFragment.findNavController(this);
        if (nav.popBackStack(R.id.onboardingFragment, false)) nav.navigate(R.id.action_onboarding_to_home);
        else if (!nav.popBackStack(R.id.homeFragment, false)) nav.navigate(R.id.homeFragment);
    }

    private void mostrarErro(CharSequence mensagem) {
        error.setText(mensagem);
        error.setVisibility(View.VISIBLE);
        error.announceForAccessibility(mensagem);
    }

    private void carregando(boolean ativo) {
        submit.setEnabled(!ativo);
        submit.setText(ativo ? R.string.loading
                : signIn ? R.string.sign_in : R.string.create_account);
    }

    void submit() {
        error.setText("");
        error.setVisibility(View.GONE);
        String nome = name.getText().toString().trim();
        String sobrenome = surname.getText().toString().trim();
        String mail = email.getText().toString().trim();
        String senha = password.getText().toString();
        if (!signIn && nome.isEmpty()) {
            mostrarErro("Informe seu nome.");
            return;
        }
        if (!signIn && sobrenome.isEmpty()) {
            mostrarErro("Informe seu sobrenome.");
            return;
        }
        if (mail.isEmpty()) {
            mostrarErro("Informe seu e-mail.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            mostrarErro("Informe um e-mail válido.");
            return;
        }
        if (signIn && senha.isEmpty()) {
            mostrarErro("Informe sua senha.");
            return;
        }
        if (!signIn && senha.length() < 8) {
            mostrarErro("A senha deve ter pelo menos 8 caracteres.");
            return;
        }
        if (chamada != null || api == null) return;
        carregando(true);
        Retorno<Sessao> retorno = new Retorno<>() {
            @Override public void sucesso(Sessao sessao) {
                chamada = null;
                if (!isAdded()) return;
                carregando(false);
                if (sessao == null) {
                    mostrarErro(getString(R.string.auth_confirm_email));
                    return;
                }
                SessaoStore.de(requireContext()).salvar(sessao.accessToken(), sessao.expiraEmSegundos(), mail);
                password.setText("");
                irParaInicio();
            }

            @Override public void falha(String mensagem) {
                chamada = null;
                if (!isAdded()) return;
                carregando(false);
                mostrarErro(mensagem);
            }
        };
        chamada = signIn ? api.entrar(mail, senha, retorno)
                : api.cadastrar(nome, sobrenome, mail, senha, retorno);
    }
}
