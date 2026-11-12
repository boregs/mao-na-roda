package com.maonaroda.api.rotas;

import com.maonaroda.api.repository.rota.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;
import com.maonaroda.api.importacao.piloto.PublicadorPiloto;
import com.maonaroda.api.integracao.rotas.CandidataRota;
import com.maonaroda.api.model.rota.ConsultaRotasResponse.Geometria;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.OffsetDateTime;
import java.util.*;

@EnabledIfEnvironmentVariable(named = "TEST_POSTGIS_URL", matches = ".+")
class PostgisIntegrationTests {
    @Test void publicacaoAtomicaPreservaVersaoAnteriorQuandoHaErro() {
        var ds = new DriverManagerDataSource(System.getenv("TEST_POSTGIS_URL"), System.getenv("TEST_POSTGIS_USER"), System.getenv("TEST_POSTGIS_PASSWORD"));
        var jdbc = new JdbcTemplate(ds);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        var publicador = new PublicadorPiloto(jdbc);
        var candidata = new CandidataRota(List.of(List.of(-46.6, -23.5), List.of(-46.601, -23.501)), 150, 120);
        var agora = OffsetDateTime.now().minusSeconds(1);
        var tipos = jdbc.queryForList("SELECT codigo FROM app.tipos_barreira ORDER BY id", String.class);
        var evidencias = tipos.stream().map(tipo -> new PublicadorPiloto.Evidencia(tipo,
                tipo.equals("escada_sem_alternativa") ? "presente" : "ausente", "dados_publicos", "fixture", "1", "teste", agora, agora, null)).toList();
        var trecho = new PublicadorPiloto.Trecho(0, 1, "calçada revisada na fixture", 0, false, false, agora, evidencias);
        var rota = new PublicadorPiloto.RotaRevisada(candidata.assinatura(), new Geometria(candidata.coordenadas()), List.of(trecho));
        String versao = "teste-" + UUID.randomUUID();
        UUID id = tx.execute(s -> publicador.publicar(new PublicadorPiloto.Dataset(versao, List.of(rota))));
        try {
            var pacote = new EvidenciaRepository(jdbc).carregar("cadeirante");
            assertEquals(versao, pacote.versao()); assertEquals(10, pacote.evidencias().size());
            assertEquals(1.0, new com.maonaroda.api.service.rota.AvaliadorAcessibilidade().avaliar(candidata, pacote.evidencias()).qualidade().cobertura());
            var invalida = new PublicadorPiloto.RotaRevisada("0".repeat(64), rota.geometria(), rota.trechos());
            assertThrows(IllegalArgumentException.class, () -> tx.execute(s -> publicador.publicar(new PublicadorPiloto.Dataset(versao + "-erro", List.of(invalida)))));
            assertEquals(versao, new EvidenciaRepository(jdbc).carregar("cadeirante").versao());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM app.datasets_rotas WHERE versao = ?", Integer.class, versao + "-erro"));
        } finally {
            tx.executeWithoutResult(s -> {
                jdbc.update("DELETE FROM app.evidencias_trecho WHERE trecho_id IN (SELECT id FROM app.trechos_pedestres WHERE dataset_id = ?)", id);
                jdbc.update("DELETE FROM app.trechos_pedestres WHERE dataset_id = ?", id);
                jdbc.update("DELETE FROM app.datasets_rotas WHERE id = ?", id);
            });
        }
    }
    @Test void catalogoMigrationsEPermissoesReais() throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("TEST_POSTGIS_URL"),
                System.getenv("TEST_POSTGIS_USER"), System.getenv("TEST_POSTGIS_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                assertEquals(10, jdbc.queryForObject("SELECT count(*) FROM app_migrations.flyway_schema_history WHERE success AND type = 'SQL'", Integer.class));
                assertEquals(30, jdbc.queryForObject("SELECT count(*) FROM app.classificacoes_barreira", Integer.class));
                jdbc.execute("SET LOCAL ROLE maonaroda_api");
                assertEquals(3, new PerfilMobilidadeRepository(jdbc).listar().size());
                assertNotNull(new EvidenciaRepository(jdbc).carregar("cadeirante"));
                assertEquals("SEM_DATASET", new EvidenciaRepository(jdbc).carregar("cadeirante").versao());
                jdbc.execute("RESET ROLE");
                String dataset = jdbc.queryForObject("INSERT INTO app.datasets_rotas(versao, versao_matriz) VALUES (?, 'RN-v1') RETURNING id::text", String.class, "teste-" + UUID.randomUUID());
                assertNotNull(dataset);
            } finally { connection.rollback(); }
        }
    }
}
