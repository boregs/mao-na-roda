package com.maonaroda.api.repository.auth;

import com.maonaroda.api.config.SessaoAtivaFilter;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consulta a sessão no Supabase Auth pela função app.sessao_ativa (V10). */
@Repository
public class SessaoRepository implements SessaoAtivaFilter.VerificadorSessao {
    private final JdbcTemplate jdbc;

    public SessaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean ativa(UUID sessao, UUID usuario) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT app.sessao_ativa(?, ?)", Boolean.class, sessao, usuario));
    }
}
