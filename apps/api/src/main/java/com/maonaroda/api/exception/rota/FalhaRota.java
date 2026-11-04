package com.maonaroda.api.exception.rota;

import org.springframework.http.HttpStatus;

public class FalhaRota extends RuntimeException {
    private final HttpStatus status;

    public FalhaRota(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
