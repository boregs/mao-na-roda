package com.maonaroda.api.repository;

import com.maonaroda.api.model.UsuarioModel;
import com.maonaroda.api.service.usuario.UsuarioService;
import com.maonaroda.api.exception.perfil.PerfilMobilidadeInvalidoException;
import com.maonaroda.api.exception.usuario.UsuarioNaoEncontradoException;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Executado apenas pelo harness local; não aceita host remoto nem banco de produção. */
@EnabledIfEnvironmentVariable(named = "MNR_TEST_JDBC_URL", matches = ".+")
class UsuarioPostgresTests {
    JdbcTemplate admin, api;
    UsuarioService service;
    UUID usuario = UUID.randomUUID(), outro = UUID.randomUUID();

    @BeforeEach void preparar() {
        String url = System.getenv("MNR_TEST_JDBC_URL");
        if (!"jdbc:postgresql://127.0.0.1:55432/maonaroda_jdbc_test".equals(url))
            throw new IllegalArgumentException("Use somente o banco descartável de testar-banco.ps1.");
        admin = new JdbcTemplate(new DriverManagerDataSource(url, "postgres", System.getenv("MNR_TEST_ADMIN_PASSWORD")));
        api = new JdbcTemplate(new DriverManagerDataSource(url, "maonaroda_jdbc_test_api", System.getenv("MNR_TEST_API_PASSWORD")));
        assertEquals("PostgreSQL", admin.execute((org.springframework.jdbc.core.ConnectionCallback<String>)
                c -> c.getMetaData().getDatabaseProductName()));
        admin.update("INSERT INTO auth.users(id) VALUES (?), (?)", usuario, outro);
        admin.update("INSERT INTO app.usuarios(auth_user_id,nome,sobrenome) VALUES (?, 'Ana', 'Teste'), (?, 'Outro', 'Teste')", usuario, outro);
        service = new UsuarioService(new UsuarioRepository(api));
    }
    @AfterEach void limpar() {
        if (admin != null) admin.update("DELETE FROM auth.users WHERE id IN (?, ?)", usuario, outro);
    }
    @Test void consultaEAtualizaSomenteIdentidadeSelecionada() {
        assertEquals("Ana", service.obter(usuario).nome());
        service.atualizar(usuario, new UsuarioModel(" Atualizado ", " Teste "), null);
        assertEquals("Atualizado", service.obter(usuario).nome());
        assertEquals("Outro", service.obter(outro).nome());
    }
    @Test void payloadSqlETratadoComoTexto() {
        String payload = "Ana', estado_conta='exclusao_em_andamento'; DROP TABLE app.usuarios; --";
        service.atualizar(usuario, new UsuarioModel(payload, "Teste"), null);
        assertEquals(payload, service.obter(usuario).nome());
        assertEquals("Outro", service.obter(outro).nome());
        assertEquals("ativa", admin.queryForObject("SELECT estado_conta FROM app.usuarios WHERE auth_user_id=?", String.class, usuario));
    }
    @Test void perfilInexistenteNaoAlteraRegistro() {
        assertThrows(PerfilMobilidadeInvalidoException.class,
                () -> service.atualizar(usuario, new UsuarioModel("Mudou", "Teste"), UUID.randomUUID()));
        assertEquals("Ana", service.obter(usuario).nome());
    }
    @Test void contaEmExclusaoNaoPodeSerAtualizada() {
        admin.update("UPDATE app.usuarios SET estado_conta = 'exclusao_em_andamento' WHERE auth_user_id = ?", usuario);
        assertThrows(UsuarioNaoEncontradoException.class,
                () -> service.atualizar(usuario, new UsuarioModel("Mudou", "Teste"), null));
        assertEquals("Outro", service.obter(outro).nome());
    }
    @Test void loginDaApiNaoEAdministradorNemOwner() {
        assertEquals(Boolean.FALSE, api.queryForObject(
                "SELECT rolsuper OR rolcreatedb OR rolcreaterole OR rolbypassrls FROM pg_roles WHERE rolname=current_user", Boolean.class));
        assertEquals(Boolean.FALSE, api.queryForObject(
                "SELECT pg_has_role(current_user, relowner, 'USAGE') FROM pg_class WHERE oid='app.usuarios'::regclass", Boolean.class));
        assertEquals(Boolean.TRUE, api.queryForObject(
                "SELECT has_table_privilege(current_user, 'app.usuarios', 'SELECT')", Boolean.class));
        assertThrows(DataAccessException.class, () -> api.update("DELETE FROM app.usuarios WHERE auth_user_id=?", usuario));
        assertThrows(DataAccessException.class, () -> api.update("UPDATE app.usuarios SET auth_user_id=? WHERE auth_user_id=?", UUID.randomUUID(), usuario));
        assertThrows(DataAccessException.class, () -> api.execute("CREATE TABLE app.teste_proibido(id integer)"));
        assertThrows(DataAccessException.class, () -> api.queryForObject("SELECT count(*) FROM auth.users", Long.class));
    }
}
