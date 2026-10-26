package com.maonaroda.api.rotas;

import org.springframework.http.HttpStatus;

public class FalhaRota extends RuntimeException {
    final HttpStatus status;

    public FalhaRota(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
}
