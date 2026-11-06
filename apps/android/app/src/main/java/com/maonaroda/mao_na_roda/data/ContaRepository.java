package com.maonaroda.mao_na_roda.data;

import com.maonaroda.mao_na_roda.data.RotasRepository.Cancelavel;
import com.maonaroda.mao_na_roda.data.RotasRepository.Retorno;

/** Cadastro, login e logout via API; o app não fala diretamente com o provedor de contas. */
public interface ContaRepository {
    /** Sessão devolvida pela API. Não há refresh token (RNF16). */
    record Sessao(String accessToken, long expiraEmSegundos) { }

    /** Sucesso com sessão null quando a conta foi criada, mas o e-mail ainda precisa ser confirmado. */
    Cancelavel cadastrar(String nome, String sobrenome, String email, String senha, Retorno<Sessao> retorno);

    Cancelavel entrar(String email, String senha, Retorno<Sessao> retorno);

    Cancelavel sair(String accessToken, Retorno<Void> retorno);
}
