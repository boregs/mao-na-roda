package com.maonaroda.api.service.usuario;

import com.maonaroda.api.model.UsuarioModel;
import com.maonaroda.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public UsuarioModel obter(UUID authUserId) {
        return repository.find(authUserId);
    }

    public void atualizar(UUID authUserId, UsuarioModel model, UUID profileUuid) {
        if (repository.update(authUserId, model, profileUuid) != 1) {
            throw new IllegalArgumentException("Usuário ou perfil não encontrado");
        }
    }

    public void iniciarExclusao(UUID authUserId) {
        if (repository.markDeleting(authUserId) != 1) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
    }
}
