package com.maonaroda.api.controller.auth;

import com.maonaroda.api.model.CriarUsuarioRequest;
import com.maonaroda.api.model.LoginUsuarioRequest;
import com.maonaroda.api.model.auth.SessaoResponse;
import com.maonaroda.api.service.auth.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacaoController {
    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    /** 201 com sessão; 202 quando o projeto Auth exigir confirmação de e-mail antes do login. */
    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CriarUsuarioRequest request) {
        return service.cadastrar(request)
                .<ResponseEntity<?>>map(sessao -> ResponseEntity.status(HttpStatus.CREATED)
                        .cacheControl(CacheControl.noStore()).body(sessao))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.ACCEPTED).cacheControl(CacheControl.noStore())
                        .body(ProblemDetail.forStatusAndDetail(HttpStatus.ACCEPTED,
                                "Conta criada. Confirme seu e-mail para entrar.")));
    }

    @PostMapping("/login")
    public ResponseEntity<SessaoResponse> entrar(@Valid @RequestBody LoginUsuarioRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.entrar(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> sair(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        service.sair(jwt.getTokenValue());
        return ResponseEntity.noContent().build();
    }
}
