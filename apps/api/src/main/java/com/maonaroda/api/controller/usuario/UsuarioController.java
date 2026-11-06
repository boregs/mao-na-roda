package com.maonaroda.api.controller.usuario;

import com.maonaroda.api.model.AtualizarUsuarioRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.maonaroda.api.model.UsuarioModel;
import com.maonaroda.api.model.UsuarioPerfilResponse;
import com.maonaroda.api.service.auth.AutenticacaoService;
import com.maonaroda.api.service.usuario.UsuarioService;

import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios/me")
public class UsuarioController {
    private final UsuarioService service;
    private final AutenticacaoService autenticacao;

    public UsuarioController(UsuarioService service, AutenticacaoService autenticacao) {
        this.service = service;
        this.autenticacao = autenticacao;
    }

    @GetMapping
    public ResponseEntity<UsuarioPerfilResponse> obter(@AuthenticationPrincipal Jwt jwt) {
        UsuarioModel usuario = service.obter(id(jwt));
        // O e-mail pertence ao Supabase Auth e não é duplicado em app.usuarios.
        return ResponseEntity.ok(new UsuarioPerfilResponse(
                usuario.uuid(), usuario.nome(), usuario.sobrenome(), jwt.getClaimAsString("email")));
    }

    @PatchMapping
    public ResponseEntity<Void> atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AtualizarUsuarioRequest request) {
        UUID usuarioId = id(jwt);
        service.atualizar(usuarioId, new UsuarioModel(request.nome(), request.sobrenome()), request.perfilMobilidadeUuid());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt) {
        autenticacao.excluirConta(id(jwt));

        return ResponseEntity.noContent().build();
    }

    private UUID id(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException erro) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identidade inválida.");
        }
    }
}
