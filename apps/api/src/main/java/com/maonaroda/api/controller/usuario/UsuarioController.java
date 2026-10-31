package com.maonaroda.api.controller.usuario;

import com.maonaroda.api.model.CriarUsuarioRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.maonaroda.api.model.UsuarioModel;
import com.maonaroda.api.service.usuario.UsuarioService;

import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios/me")
public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<UsuarioModel> obter(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.obter(id(jwt)));
    }

    @PatchMapping
    public ResponseEntity<Void> atualizar(@AuthenticationPrincipal Jwt jwt, @RequestBody UsuarioModel model) {
        UUID usuarioId = id(jwt);
        if (model.nome() == null || model.nome().isBlank() || model.nome().length() > 100
                || model.sobrenome() == null || model.sobrenome().isBlank() || model.sobrenome().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome e sobrenome de até 100 caracteres.");
        }
        service.atualizar(usuarioId, model, null);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt) {
        service.iniciarExclusao(id(jwt));

        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Void> criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CriarUsuarioRequest request) {
        id(jwt);
        // Retornar 201 Created somente após implementar e concluir a criação.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
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
