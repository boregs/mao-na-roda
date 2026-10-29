package com.maonaroda.api.controller.usuario;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.maonaroda.api.model.UsuarioModel;
import com.maonaroda.api.service.usuario.UsuarioService;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios/me")
public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public UsuarioModel obter(@AuthenticationPrincipal Jwt jwt) {
        return service.obter(id(jwt));
    }

    @PatchMapping
    public void atualizar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UsuarioModel model) {
        service.atualizar(id(jwt), model, null);
    }

    @DeleteMapping
    public void excluir(@AuthenticationPrincipal Jwt jwt) {
        service.iniciarExclusao(id(jwt));
    }

    private UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
