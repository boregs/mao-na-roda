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
        return comLimiteDeTentativas(email, () -> auth.entrar(email, request.senha()));
    }

    /**
     * RNF18 para qualquer conferência de senha (login e exclusão da conta): reserva a tentativa,
     * confirma a falha só para credenciais inválidas e zera o contador no sucesso.
     */
    private <T> T comLimiteDeTentativas(String email, java.util.function.Supplier<T> tentativa) {
        String hash = hash(email);
        if (!tentativas.reservar(hash))
            throw new FalhaAutenticacao(Tipo.BLOQUEADO, tentativas.segundosAteLiberar(hash));

        T resultado;
        try {
            resultado = tentativa.get();
        } catch (FalhaAutenticacao erro) {
            if (erro.tipo() == Tipo.CREDENCIAIS_INVALIDAS) tentativas.registrarFalha(hash);
            else tentativas.liberar(hash);
            throw erro;
        } catch (RuntimeException erro) {
            tentativas.liberar(hash);
            throw erro;
        }
        tentativas.limpar(hash);
        return resultado;
    }

    public void sair(String accessToken) {
        auth.sair(accessToken);
    }

    /**
     * RF12 / UC10 A2: confirma a senha da própria conta e remove a identidade no Auth; o banco
     * remove sessões e cadastro em cascata. O e-mail vem do token, nunca do corpo da requisição.
     * O sucesso da senha também apaga a linha de tentativas desse e-mail (RN16).
     */
    public void excluirConta(java.util.UUID usuario, String emailDoToken, String senha) {
        String email = normalizar(emailDoToken);
        java.util.UUID dono;
        try {
            dono = comLimiteDeTentativas(email, () -> auth.confirmarSenha(email, senha));
        } catch (FalhaAutenticacao erro) {
            if (erro.tipo() == Tipo.CREDENCIAIS_INVALIDAS) throw new FalhaAutenticacao(Tipo.SENHA_INCORRETA);
            throw erro;
        }
        if (!usuario.equals(dono)) throw new FalhaAutenticacao(Tipo.SENHA_INCORRETA);
        auth.excluirUsuario(usuario);
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
