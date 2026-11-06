package com.maonaroda.api.service.auth;

import com.maonaroda.api.exception.auth.FalhaAutenticacao;
import com.maonaroda.api.exception.auth.FalhaAutenticacao.Tipo;
import com.maonaroda.api.integracao.supabase.SupabaseAuthClient;
import com.maonaroda.api.model.CriarUsuarioRequest;
import com.maonaroda.api.model.LoginUsuarioRequest;
import com.maonaroda.api.model.auth.SessaoResponse;
import com.maonaroda.api.repository.auth.TentativasLoginRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService {
    private final SupabaseAuthClient auth;
    private final TentativasLoginRepository tentativas;

    public AutenticacaoService(SupabaseAuthClient auth, TentativasLoginRepository tentativas) {
        this.auth = auth;
        this.tentativas = tentativas;
    }

    /** Cria a conta; o cadastro em app.usuarios é feito pelo trigger da V8 na mesma transação. */
    public Optional<SessaoResponse> cadastrar(CriarUsuarioRequest request) {
        return auth.cadastrar(normalizar(request.email()), request.senha(),
                request.nome().trim(), request.sobrenome().trim());
    }

    public SessaoResponse entrar(LoginUsuarioRequest request) {
        String email = normalizar(request.email());
        String hash = hash(email);
        if (!tentativas.reservar(hash))
            throw new FalhaAutenticacao(Tipo.BLOQUEADO, tentativas.segundosAteLiberar(hash));

        SessaoResponse sessao;
        try {
            sessao = auth.entrar(email, request.senha());
        } catch (FalhaAutenticacao erro) {
            if (erro.tipo() == Tipo.CREDENCIAIS_INVALIDAS) tentativas.registrarFalha(hash);
            else tentativas.liberar(hash);
            throw erro;
        } catch (RuntimeException erro) {
            tentativas.liberar(hash);
            throw erro;
        }
        tentativas.limpar(hash);
        return sessao;
    }

    public void sair(String accessToken) {
        auth.sair(accessToken);
    }

    static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String hash(String emailNormalizado) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(emailNormalizado.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException(erro);
        }
    }
}
