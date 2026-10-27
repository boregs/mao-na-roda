package com.maonaroda.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados necessários para criar uma identidade no provedor de autenticação.
 * A senha só pode existir durante a requisição e nunca deve ser persistida.
 */
public record CriarUsuarioRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 100) String sobrenome,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 128) String senha) {
}
