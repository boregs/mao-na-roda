package com.maonaroda.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginUsuarioRequest(
        @NotBlank @Email String email,
        @NotBlank String senha) {
}
