package com.maonaroda.api.controller.usuario;

import com.maonaroda.api.controller.auth.AutenticacaoExceptionHandler;
import com.maonaroda.api.exception.auth.FalhaAutenticacao;
import com.maonaroda.api.exception.usuario.UsuarioNaoEncontradoException;
import com.maonaroda.api.exception.perfil.PerfilMobilidadeInvalidoException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UsuarioController.class)
public class UsuarioExceptionHandler {
    @ExceptionHandler(PerfilMobilidadeInvalidoException.class)
    ResponseEntity<ProblemDetail> perfilInvalido() {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Perfil de mobilidade inexistente ou inativo."));
    }

    @ExceptionHandler(FalhaAutenticacao.class)
    ResponseEntity<ProblemDetail> autenticacao(FalhaAutenticacao erro) {
        return AutenticacaoExceptionHandler.resposta(erro);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> camposInvalidos(org.springframework.web.bind.MethodArgumentNotValidException erro) {
        var corpo = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revise os campos informados.");
        corpo.setProperty("campos", erro.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.validation.FieldError::getField).distinct().sorted().toList());
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> jsonInvalido() {
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "JSON inválido."));
    }

    @ExceptionHandler({UsuarioNaoEncontradoException.class, EmptyResultDataAccessException.class})
    ResponseEntity<ProblemDetail> naoEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ProblemDetail> inesperado(RuntimeException erro) {
        // Preserve os erros HTTP e de validação tratados pelo Spring MVC.
        if (erro instanceof org.springframework.web.ErrorResponse) {
            throw erro;
        }
        return ResponseEntity.internalServerError()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Não foi possível concluir a operação."));
    }
}
