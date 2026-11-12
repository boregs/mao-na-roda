package com.maonaroda.api.repository.auth;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Usa o banco migrado do CI: tabela da V9, grants/RLS de maonaroda_api e o SQL do RNF18. */
@EnabledIfEnvironmentVariable(named = "TEST_POSTGIS_URL", matches = ".+")
class TentativasLoginPostgresTests {
    JdbcTemplate admin;
    final String hash = java.util.HexFormat.of().formatHex(java.security.SecureRandom.getSeed(32));

    @BeforeEach void preparar() {
        admin = new JdbcTemplate(new DriverManagerDataSource(System.getenv("TEST_POSTGIS_URL"),
                System.getenv("TEST_POSTGIS_USER"), System.getenv("TEST_POSTGIS_PASSWORD")));
    }

    @AfterEach void limpar() {
        admin.update("DELETE FROM app.tentativas_login WHERE email_hash = ?", hash);
    }

    /** Conexão com o papel da API, sujeita aos grants e às políticas RLS. */
    private SingleConnectionDataSource comoApi() {
        var ds = new SingleConnectionDataSource(System.getenv("TEST_POSTGIS_URL"),
                System.getenv("TEST_POSTGIS_USER"), System.getenv("TEST_POSTGIS_PASSWORD"), true);
        new JdbcTemplate(ds).execute("SET ROLE maonaroda_api");
        return ds;
    }

    @Test void cincoFalhasBloqueiamQuinzeMinutosESucessoZera() {
        var ds = comoApi();
        try {
            var repo = new TentativasLoginRepository(new JdbcTemplate(ds));
            for (int i = 0; i < 4; i++) {
                assertTrue(repo.reservar(hash));
                repo.registrarFalha(hash);
            }
            // Sucesso na quinta tentativa zera o contador.
            assertTrue(repo.reservar(hash));
            repo.limpar(hash);
            for (int i = 0; i < 5; i++) {
                assertTrue(repo.reservar(hash), "tentativa " + (i + 1));
                repo.registrarFalha(hash);
            }
            assertFalse(repo.reservar(hash));
            long restante = repo.segundosAteLiberar(hash);
            assertTrue(restante > 880 && restante <= 900, "restante=" + restante);

            // Depois do bloqueio, a janela recomeça.
            admin.update("UPDATE app.tentativas_login SET janela_inicio = now() - interval '31 minutes', "
                    + "bloqueado_ate = now() - interval '1 second' WHERE email_hash = ?", hash);
            assertTrue(repo.reservar(hash));
            assertEquals(1, admin.queryForObject("SELECT tentativas FROM app.tentativas_login WHERE email_hash = ?",
                    Integer.class, hash));
        } finally {
            ds.destroy();
        }
    }

    @Test void falhasAntigasSaemDaJanela() {
        var ds = comoApi();
        try {
            var repo = new TentativasLoginRepository(new JdbcTemplate(ds));
            for (int i = 0; i < 4; i++) { repo.reservar(hash); repo.registrarFalha(hash); }
            admin.update("UPDATE app.tentativas_login SET janela_inicio = now() - interval '16 minutes' "
                    + "WHERE email_hash = ?", hash);
            for (int i = 0; i < 4; i++) { assertTrue(repo.reservar(hash)); repo.registrarFalha(hash); }
            assertTrue(repo.reservar(hash));
        } finally {
            ds.destroy();
        }
    }

    @Test void indisponibilidadeDevolveATentativa() {
        var ds = comoApi();
        try {
            var repo = new TentativasLoginRepository(new JdbcTemplate(ds));
            for (int i = 0; i < 10; i++) { assertTrue(repo.reservar(hash)); repo.liberar(hash); }
        } finally {
            ds.destroy();
        }
    }

    @Test void tentativasSimultaneasNaoUltrapassamOLimite() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        try {
            var tarefas = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 20; i++) tarefas.add(() -> {
                var ds = comoApi();
                try { return new TentativasLoginRepository(new JdbcTemplate(ds)).reservar(hash); }
                finally { ds.destroy(); }
            });
            long permitidas = 0;
            for (var f : pool.invokeAll(tarefas)) if (f.get()) permitidas++;
            assertEquals(TentativasLoginRepository.LIMITE, permitidas);
        } finally {
            pool.shutdownNow();
        }
    }
}
