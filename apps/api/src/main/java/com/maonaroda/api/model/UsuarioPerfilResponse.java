package com.maonaroda.api.model;

import java.util.UUID;

/**
 * Perfil público da conta. O e-mail é fornecido pelo Auth quando necessário;
 * a senha nunca faz parte de uma resposta da API.
 */
public record UsuarioPerfilResponse(
        UUID uuid,
        String nome,
        String sobrenome,
        String email) {
}
