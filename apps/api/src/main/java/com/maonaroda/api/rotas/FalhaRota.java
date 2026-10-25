package com.maonaroda.api.rotas;

import org.springframework.http.HttpStatus;

class FalhaRota extends RuntimeException {
    final HttpStatus status;

    FalhaRota(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
}
