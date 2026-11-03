package com.maonaroda.api.repository;

import com.maonaroda.api.model.UsuarioModel;
import com.maonaroda.api.exception.perfil.PerfilMobilidadeInvalidoException;
import com.maonaroda.api.exception.usuario.UsuarioNaoEncontradoException;
import com.maonaroda.api.service.usuario.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Banco em memória exclusivo por teste; não executa as migrations PostgreSQL/PostGIS.
class UsuarioRepositoryTests {
    private JdbcTemplate jdbc;
    private UsuarioService service;
    private final UUID usuario = UUID.randomUUID();
    private final UUID outroUsuario = UUID.randomUUID();
    private final UUID perfil = UUID.randomUUID();
    private final UUID novoPerfil = UUID.randomUUID();
    private final UsuarioModel nomes = new UsuarioModel(" Ana ", " Silva ");

    @BeforeEach
    void preparar() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA app");
        jdbc.execute("CREATE TABLE app.perfis_mobilidade (id BIGINT PRIMARY KEY, uuid UUID UNIQUE, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE app.usuarios (auth_user_id UUID PRIMARY KEY, nome TEXT, sobrenome TEXT, "
                + "perfil_mobilidade_id BIGINT REFERENCES app.perfis_mobilidade(id), estado_conta TEXT)");
        jdbc.update("INSERT INTO app.perfis_mobilidade VALUES (1, ?, true), (2, ?, true)", perfil, novoPerfil);
        jdbc.update("INSERT INTO app.usuarios VALUES (?, 'Original', 'Nome', 1, 'ativa'), "
                + "(?, 'Outro', 'Usuario', 1, 'ativa')", usuario, outroUsuario);
        service = new UsuarioService(new UsuarioRepository(jdbc));
    }

    private Long perfilAtual(UUID id) {
        return jdbc.queryForObject("SELECT perfil_mobilidade_id FROM app.usuarios WHERE auth_user_id = ?", Long.class, id);
    }

    @Test void alterarNomesPreservaPerfilEOutrosUsuarios() {
        service.atualizar(usuario, nomes, null);
        assertEquals(1L, perfilAtual(usuario));
        assertEquals("Ana", service.obter(usuario).nome());
        assertEquals("Silva", service.obter(usuario).sobrenome());
        assertEquals("Outro", service.obter(outroUsuario).nome());
    }

    @Test void permiteAlterarPerfilAtivo() {
        service.atualizar(usuario, nomes, novoPerfil);
        assertEquals(2L, perfilAtual(usuario));
        assertEquals(1L, perfilAtual(outroUsuario));
    }

    @Test void recusaPerfilInexistenteSemAlterarDados() {
        assertThrows(PerfilMobilidadeInvalidoException.class,
                () -> service.atualizar(usuario, nomes, UUID.randomUUID()));
        assertEquals(1L, perfilAtual(usuario));
        assertEquals("Original", service.obter(usuario).nome());
    }

    @Test void recusaPerfilInativoSemAlterarDados() {
        jdbc.update("UPDATE app.perfis_mobilidade SET ativo = false WHERE uuid = ?", novoPerfil);
        assertThrows(PerfilMobilidadeInvalidoException.class,
                () -> service.atualizar(usuario, nomes, novoPerfil));
        assertEquals(1L, perfilAtual(usuario));
        assertEquals("Original", service.obter(usuario).nome());
    }

    @Test void repositoryNaoGravaPerfilInexistenteMesmoSemService() {
        assertEquals(0, new UsuarioRepository(jdbc).update(usuario, nomes, UUID.randomUUID()));
        assertEquals(1L, perfilAtual(usuario));
    }

    @Test void recusaUsuarioAusenteOuEmExclusao() {
        assertThrows(UsuarioNaoEncontradoException.class,
                () -> service.atualizar(UUID.randomUUID(), nomes, null));
        service.iniciarExclusao(usuario);
        assertThrows(UsuarioNaoEncontradoException.class,
                () -> service.atualizar(usuario, nomes, novoPerfil));
        assertEquals(1L, perfilAtual(usuario));
    }
}
