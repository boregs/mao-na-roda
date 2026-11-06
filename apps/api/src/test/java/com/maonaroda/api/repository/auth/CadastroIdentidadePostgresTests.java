package com.maonaroda.api.repository.auth;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Usa o banco migrado do CI: trigger da V8 que cria o cadastro junto da conta Auth. */
@EnabledIfEnvironmentVariable(named = "TEST_POSTGIS_URL", matches = ".+")
class CadastroIdentidadePostgresTests {
    JdbcTemplate admin;
    final UUID identidade = UUID.randomUUID();

    @BeforeEach void preparar() {
        admin = new JdbcTemplate(new DriverManagerDataSource(System.getenv("TEST_POSTGIS_URL"),
                System.getenv("TEST_POSTGIS_USER"), System.getenv("TEST_POSTGIS_PASSWORD")));
    }

    @AfterEach void limpar() {
        admin.update("DELETE FROM auth.users WHERE id = ?", identidade);
    }

    @Test void contaComMetadadosGanhaCadastroNaMesmaTransacao() {
        admin.update("INSERT INTO auth.users (id, raw_user_meta_data) VALUES (?, ?::jsonb)", identidade,
                "{\"nome\":\"  Ana \",\"sobrenome\":\"Silva\"}");
        var linha = admin.queryForMap("SELECT nome, sobrenome, estado_conta FROM app.usuarios WHERE auth_user_id = ?",
                identidade);
        assertEquals("Ana", linha.get("nome"));
        assertEquals("Silva", linha.get("sobrenome"));
        assertEquals("ativa", linha.get("estado_conta"));
    }

    @Test void contaSemMetadadosContinuaSemCadastro() {
        admin.update("INSERT INTO auth.users (id, raw_user_meta_data) VALUES (?, ?::jsonb)", identidade,
                "{\"nome\":\"\\t\"}");
        assertEquals(0, admin.queryForObject("SELECT count(*) FROM app.usuarios WHERE auth_user_id = ?",
                Integer.class, identidade));
    }
}
