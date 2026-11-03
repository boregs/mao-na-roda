package com.maonaroda.api.repository.rota;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PerfilMobilidadeRepository {
    private final JdbcTemplate jdbc;
    public PerfilMobilidadeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Perfil(String codigo, String nome) { }

    public List<Perfil> listar() {
        return jdbc.query("SELECT codigo, nome FROM app.perfis_mobilidade WHERE ativo ORDER BY id",
                (rs, row) -> new Perfil(rs.getString(1), rs.getString(2)));
    }
}
