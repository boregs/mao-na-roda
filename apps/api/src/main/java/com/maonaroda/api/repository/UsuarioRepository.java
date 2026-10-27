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
                "SELECT nome, sobrenome "
                        + "FROM app.usuarios "
                        + "WHERE auth_user_id = ? AND estado_conta = 'ativa'",
                (rs, row) -> new UsuarioModel(
                        rs.getString("nome"),
                        rs.getString("sobrenome")),
                authUserId);
    }

    public int update(UUID authUserId, UsuarioModel model, UUID profileUuid) {
        return jdbc.update(
                "UPDATE app.usuarios "
                        + "SET nome = ?, sobrenome = ?, "
                        + "perfil_mobilidade_id = ("
                        + "SELECT id FROM app.perfis_mobilidade "
                        + "WHERE uuid = ? AND ativo) "
                        + "WHERE auth_user_id = ? "
                        + "AND estado_conta = 'ativa'",
                model.nome().trim(),
                model.sobrenome().trim(),
                profileUuid,
                authUserId);
    }

    public int markDeleting(UUID authUserId) {
        return jdbc.update(
                "UPDATE app.usuarios "
                        + "SET estado_conta = 'exclusao_em_andamento' "
                        + "WHERE auth_user_id = ? "
                        + "AND estado_conta = 'ativa'",
                authUserId);
    }
}
