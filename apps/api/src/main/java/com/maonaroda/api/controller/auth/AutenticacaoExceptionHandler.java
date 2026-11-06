package com.maonaroda.api.controller.auth;

import com.maonaroda.api.exception.auth.FalhaAutenticacao;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AutenticacaoController.class)
public class AutenticacaoExceptionHandler {
    @ExceptionHandler(FalhaAutenticacao.class)
    ResponseEntity<ProblemDetail> falha(FalhaAutenticacao erro) {
        return resposta(erro);
    }

    /** Também usado pelos recursos de usuário que dependem do Auth (exclusão da conta). */
    public static ResponseEntity<ProblemDetail> resposta(FalhaAutenticacao erro) {
        HttpStatus status = switch (erro.tipo()) {
            case CREDENCIAIS_INVALIDAS -> HttpStatus.UNAUTHORIZED;
            case CONTA_EXISTENTE -> HttpStatus.CONFLICT;
            case SENHA_FRACA, EMAIL_INVALIDO -> HttpStatus.BAD_REQUEST;
            case EMAIL_NAO_CONFIRMADO -> HttpStatus.FORBIDDEN;
            case BLOQUEADO, LIMITE_PROVEDOR -> HttpStatus.TOO_MANY_REQUESTS;
            case INDISPONIVEL -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        String detalhe = switch (erro.tipo()) {
            // UC07 A1: não indicar se o e-mail ou a senha está incorreto.
            case CREDENCIAIS_INVALIDAS -> "Não foi possível entrar. Confira o e-mail e a senha.";
            case CONTA_EXISTENTE -> "Já existe uma conta com este e-mail. Entre com sua senha.";
            case SENHA_FRACA -> "A senha não atende aos requisitos de segurança.";
            case EMAIL_INVALIDO -> "Informe um e-mail válido.";
            case EMAIL_NAO_CONFIRMADO -> "Confirme seu e-mail antes de entrar.";
            case BLOQUEADO -> "Muitas tentativas de login. Tente novamente mais tarde.";
            case LIMITE_PROVEDOR -> "Muitas solicitações. Aguarde um minuto e tente novamente.";
            case INDISPONIVEL -> "O serviço de contas está indisponível. Tente novamente.";
        };
        var corpo = ProblemDetail.forStatusAndDetail(status, detalhe);
        corpo.setProperty("codigo", erro.tipo().name());
        var resposta = ResponseEntity.status(status).cacheControl(CacheControl.noStore());
        if (erro.tentarNovamenteEmSegundos() > 0) {
            corpo.setProperty("tentarNovamenteEmSegundos", erro.tentarNovamenteEmSegundos());
            resposta.header("Retry-After", Long.toString(erro.tentarNovamenteEmSegundos()));
        }
        return resposta.body(corpo);
    }

    /** Respondido aqui: sem isso o Spring encaminharia para /error, que a segurança recusa com 401. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> camposInvalidos(MethodArgumentNotValidException erro) {
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

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemDetail> bancoIndisponivel() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "O serviço de contas está indisponível. Tente novamente."));
    }
}
