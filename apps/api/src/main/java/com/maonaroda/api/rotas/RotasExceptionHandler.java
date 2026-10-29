package com.maonaroda.api.rotas;

import com.maonaroda.api.controller.RotasController;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = RotasController.class)
public class RotasExceptionHandler {
    @ExceptionHandler(FalhaRota.class)
    ResponseEntity<ProblemDetail> falha(FalhaRota erro) {
        return ResponseEntity.status(erro.status)
                .body(ProblemDetail.forStatusAndDetail(erro.status, erro.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> invalido(IllegalArgumentException erro) {
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> jsonInvalido() {
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "JSON inválido. Informe origem e destino com endereço ou coordenadas numéricas."));
    }
}
