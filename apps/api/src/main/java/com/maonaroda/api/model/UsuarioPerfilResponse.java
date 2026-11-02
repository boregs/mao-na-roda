package com.maonaroda.api.model;

import java.util.UUID;

public record UsuarioPerfilResponse(
        UUID uuid,
        String nome,
        String sobrenome,
        String email) {
}
