package com.maonaroda.api.exception.auth;

/** Resultado não bem-sucedido de cadastro, login ou logout; a mensagem nunca vem do provedor. */
public class FalhaAutenticacao extends RuntimeException {
    public enum Tipo {
        CREDENCIAIS_INVALIDAS,
        SENHA_INCORRETA,
        CONTA_EXISTENTE,
        SENHA_FRACA,
        EMAIL_INVALIDO,
        EMAIL_NAO_CONFIRMADO,
        BLOQUEADO,
        LIMITE_PROVEDOR,
        INDISPONIVEL
    }

    private final Tipo tipo;
    private final long tentarNovamenteEmSegundos;

    public FalhaAutenticacao(Tipo tipo) {
        this(tipo, 0);
    }

    public FalhaAutenticacao(Tipo tipo, long tentarNovamenteEmSegundos) {
        super(tipo.name());
        this.tipo = tipo;
        this.tentarNovamenteEmSegundos = tentarNovamenteEmSegundos;
    }

    public Tipo tipo() {
        return tipo;
    }

    public long tentarNovamenteEmSegundos() {
        return tentarNovamenteEmSegundos;
    }
}
