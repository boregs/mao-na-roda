package com.maonaroda.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AtualizarUsuarioRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 100) String sobrenome,
        UUID perfilMobilidadeUuid) {
}
