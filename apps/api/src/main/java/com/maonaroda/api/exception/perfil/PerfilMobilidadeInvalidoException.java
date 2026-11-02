package com.maonaroda.api.exception.perfil;

public class PerfilMobilidadeInvalidoException extends RuntimeException {
    public PerfilMobilidadeInvalidoException() {
        super("Perfil de mobilidade inexistente ou inativo.");
    }
}
