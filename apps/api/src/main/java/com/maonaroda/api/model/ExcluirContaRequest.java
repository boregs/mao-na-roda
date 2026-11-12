package com.maonaroda.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Confirmação da senha antes de excluir a conta (UC10 A2). */
public record ExcluirContaRequest(@NotBlank @Size(max = 128) String senha) {
}
