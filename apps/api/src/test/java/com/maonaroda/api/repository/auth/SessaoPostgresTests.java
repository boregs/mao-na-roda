package com.maonaroda.api.repository.auth;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Função app.sessao_ativa (V10) chamada com o papel da API, sem acesso direto a auth. */
@EnabledIfEnvironmentVariable(named = "TEST_POSTGIS_URL", matches = ".+")
class SessaoPostgresTests {
    JdbcTemplate admin;
    SingleConnectionDataSource api;
    SessaoRepository sessoes;
    final UUID usuario = UUID.randomUUID(), outro = UUID.randomUUID(), sessao = UUID.randomUUID();

    @BeforeEach void preparar() {
        String url = System.getenv("TEST_POSTGIS_URL"), user = System.getenv("TEST_POSTGIS_USER"),
                senha = System.getenv("TEST_POSTGIS_PASSWORD");
        admin = new JdbcTemplate(new DriverManagerDataSource(url, user, senha));
        admin.update("INSERT INTO auth.users (id) VALUES (?), (?)", usuario, outro);
        admin.update("INSERT INTO auth.sessions (id, user_id) VALUES (?, ?)", sessao, usuario);
        api = new SingleConnectionDataSource(url, user, senha, true);
        new JdbcTemplate(api).execute("SET ROLE maonaroda_api");
        sessoes = new SessaoRepository(new JdbcTemplate(api));
    }

    @AfterEach void limpar() {
        if (api != null) api.destroy();
        admin.update("DELETE FROM auth.users WHERE id IN (?, ?)", usuario, outro);
    }

    @Test void sessaoExistenteDoMesmoUsuarioEstaAtiva() {
        assertTrue(sessoes.ativa(sessao, usuario));
        assertFalse(sessoes.ativa(sessao, outro), "sessão de outro usuário");
        assertFalse(sessoes.ativa(UUID.randomUUID(), usuario), "sessão inexistente");
    }

    @Test void sessaoComPrazoVencidoNaoEstaAtiva() {
        admin.update("UPDATE auth.sessions SET not_after = now() - interval '1 second' WHERE id = ?", sessao);
        assertFalse(sessoes.ativa(sessao, usuario));
        admin.update("UPDATE auth.sessions SET not_after = now() + interval '1 hour' WHERE id = ?", sessao);
        assertTrue(sessoes.ativa(sessao, usuario));
    }

    @Test void logoutOuExclusaoDaContaEncerramASessao() {
        admin.update("DELETE FROM auth.sessions WHERE id = ?", sessao);
        assertFalse(sessoes.ativa(sessao, usuario));
        admin.update("INSERT INTO auth.sessions (id, user_id) VALUES (?, ?)", sessao, usuario);
        admin.update("DELETE FROM auth.users WHERE id = ?", usuario);
        assertFalse(sessoes.ativa(sessao, usuario));
    }

    @Test void apiNaoLeAuthSessionsDiretamente() {
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> new JdbcTemplate(api).queryForObject("SELECT count(*) FROM auth.sessions", Integer.class));
    }
}
