package com.maonaroda.api.repository;

import com.maonaroda.api.model.UsuarioModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class UsuarioRepository {
    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UsuarioModel find(UUID authUserId) {
        return jdbc.queryForObject(
                "SELECT uuid, nome, sobrenome "
                        + "FROM app.usuarios "
                        + "WHERE auth_user_id = ? AND estado_conta = 'ativa'",
                (rs, row) -> new UsuarioModel(null, rs.getObject("uuid", UUID.class), null,
                        rs.getString("nome"), rs.getString("sobrenome"), null, null, null, null),
                authUserId);
    }

    public int update(UUID authUserId, UsuarioModel model, UUID profileUuid) {
        if (profileUuid == null) {
            return jdbc.update(
                    "UPDATE app.usuarios SET nome = ?, sobrenome = ? "
                            + "WHERE auth_user_id = ? AND estado_conta = 'ativa'",
                    model.nome().trim(), model.sobrenome().trim(), authUserId);
        }

        return jdbc.update(
                "UPDATE app.usuarios "
                        + "SET nome = ?, sobrenome = ?, "
                        + "perfil_mobilidade_id = ("
                        + "SELECT id FROM app.perfis_mobilidade "
                        + "WHERE uuid = ? AND ativo) "
                        + "WHERE auth_user_id = ? "
                        + "AND estado_conta = 'ativa' "
                        + "AND EXISTS (SELECT 1 FROM app.perfis_mobilidade WHERE uuid = ? AND ativo)",
                model.nome().trim(),
                model.sobrenome().trim(),
                profileUuid,
                authUserId,
                profileUuid);
    }

    public boolean perfilAtivo(UUID profileUuid) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM app.perfis_mobilidade WHERE uuid = ? AND ativo)",
                Boolean.class, profileUuid));
    }
}
