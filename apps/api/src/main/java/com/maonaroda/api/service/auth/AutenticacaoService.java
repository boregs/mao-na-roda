package com.maonaroda.api.service.auth;

import com.maonaroda.api.integracao.supabase.SupabaseAuthClient;
import com.maonaroda.api.model.CriarUsuarioRequest;
import com.maonaroda.api.model.LoginUsuarioRequest;
import com.maonaroda.api.model.auth.SessaoResponse;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService {
    private final SupabaseAuthClient auth;

    public AutenticacaoService(SupabaseAuthClient auth) {
        this.auth = auth;
    }

    /** Cria a conta; o cadastro em app.usuarios é feito pelo trigger da V8 na mesma transação. */
    public Optional<SessaoResponse> cadastrar(CriarUsuarioRequest request) {
        return auth.cadastrar(normalizar(request.email()), request.senha(),
                request.nome().trim(), request.sobrenome().trim());
    }

    public SessaoResponse entrar(LoginUsuarioRequest request) {
        return auth.entrar(normalizar(request.email()), request.senha());
    }

    public void sair(String accessToken) {
        auth.sair(accessToken);
    }

    static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
