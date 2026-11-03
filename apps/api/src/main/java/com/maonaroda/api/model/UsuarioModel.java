package com.maonaroda.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record UsuarioModel(
        Long id,
        UUID uuid,
        UUID authUserId,
        String nome,
        String sobrenome,
        String email,
        Long perfilMobilidadeId,
        String estadoConta,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String senha) {

    public UsuarioModel(String nome, String sobrenome) {
        this(null, null, null, nome, sobrenome, null, null, null, null);
    }
}
