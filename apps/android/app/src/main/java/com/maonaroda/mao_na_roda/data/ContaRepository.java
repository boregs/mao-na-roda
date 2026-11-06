package com.maonaroda.mao_na_roda.data;

import com.maonaroda.mao_na_roda.data.RotasRepository.Cancelavel;
import com.maonaroda.mao_na_roda.data.RotasRepository.Retorno;

/** Cadastro, login e logout via API; o app não fala diretamente com o provedor de contas. */
public interface ContaRepository {
    /**
     * Mensagem entregue quando a API responde 401 numa chamada protegida: a sessão terminou
     * (logout em outro lugar, conta excluída ou expiração) e o aparelho deve encerrá-la.
     * O login e a senha incorreta na exclusão não usam 401 com este sentido.
     */
    String SESSAO_ENCERRADA = "Sua sessão terminou. Entre novamente.";

    /** Sessão devolvida pela API. Não há refresh token (RNF16). */
    record Sessao(String accessToken, long expiraEmSegundos) { }

    /** Sucesso com sessão null quando a conta foi criada, mas o e-mail ainda precisa ser confirmado. */
    Cancelavel cadastrar(String nome, String sobrenome, String email, String senha, Retorno<Sessao> retorno);

    Cancelavel entrar(String email, String senha, Retorno<Sessao> retorno);

    Cancelavel sair(String accessToken, Retorno<Void> retorno);
}
