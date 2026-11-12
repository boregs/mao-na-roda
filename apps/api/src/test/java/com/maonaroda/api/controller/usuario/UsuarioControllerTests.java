package com.maonaroda.api.controller.usuario;

import com.maonaroda.api.model.UsuarioModel;
import com.maonaroda.api.service.usuario.UsuarioService;
import com.maonaroda.api.exception.usuario.UsuarioNaoEncontradoException;
import com.maonaroda.api.exception.perfil.PerfilMobilidadeInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsuarioControllerTests {
    private static final String URL = "/api/v1/usuarios/me";
    private static final UUID ID = UUID.randomUUID();
    private static final String NOME = "{\"nome\":\"Ana\",\"sobrenome\":\"Silva\"}";
    private static final UUID PUBLICO = UUID.randomUUID();
    private Jwt principal;
    private RuntimeException falha;
    private UUID chamado;
    private UUID excluido;
    private RuntimeException falhaAuth;
    private String emailExclusao, senhaExclusao;
    private static final String EXCLUSAO = URL + "/exclusao";
    private static final String SENHA = "{\"senha\":\"senha12345\"}";
    private UUID perfilChamado;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        principal = Jwt.withTokenValue("teste").header("alg", "RS256").subject(ID.toString())
                .claim("email", "ana@example.com").build();
        UsuarioService service = new UsuarioService(null) {
            private void registrar(UUID id) {
                chamado = id;
                if (falha != null) throw falha;
            }
            @Override public UsuarioModel obter(UUID id) {
                registrar(id);
                return new UsuarioModel(null, PUBLICO, null, "Ana", "Silva", null, null, null, null);
            }
            @Override public void atualizar(UUID id, UsuarioModel model, UUID perfil) {
                registrar(id);
                perfilChamado = perfil;
            }
        };
        var autenticacao = new com.maonaroda.api.service.auth.AutenticacaoService(null, null) {
            @Override public void excluirConta(UUID id, String email, String senha) {
                excluido = id; emailExclusao = email; senhaExclusao = senha;
                if (falhaAuth != null) throw falhaAuth;
            }
        };
        mvc = MockMvcBuilders.standaloneSetup(new UsuarioController(service, autenticacao))
                .setControllerAdvice(new UsuarioExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == Jwt.class;
                    }
                    @Override public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory factory) { return principal; }
                }).build();
    }

    @Test void obterRetornaSomenteContratoPublico() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(PUBLICO.toString()))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.sobrenome").value("Silva"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.authUserId").doesNotExist())
                .andExpect(jsonPath("$.estadoConta").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());
        assertEquals(ID, chamado);
    }

    @Test void atualizarEExcluirRetornamSemConteudo() throws Exception {
        mvc.perform(patch(URL).contentType("application/json").content(NOME))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertEquals(ID, chamado);
        mvc.perform(post(EXCLUSAO).contentType("application/json").content(SENHA))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertEquals(ID, excluido);
        assertEquals("ana@example.com", emailExclusao);
        assertEquals("senha12345", senhaExclusao);
    }

    @Test void cadastroNaoEhMaisFeitoPorEsteRecurso() throws Exception {
        // O cadastro foi para POST /api/v1/auth/cadastro.
        mvc.perform(post(URL).contentType("application/json").content(NOME))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test void encaminhaPerfilExplicitoEValidaUuid() throws Exception {
        UUID perfil = UUID.randomUUID();
        mvc.perform(patch(URL).contentType("application/json").content(
                "{\"nome\":\"Ana\",\"sobrenome\":\"Silva\",\"perfilMobilidadeUuid\":\"" + perfil + "\"}"))
                .andExpect(status().isNoContent());
        assertEquals(perfil, perfilChamado);
        mvc.perform(patch(URL).contentType("application/json").content(
                "{\"nome\":\"Ana\",\"sobrenome\":\"Silva\",\"perfilMobilidadeUuid\":\"invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void perfilAusenteOuInativoRetorna400() throws Exception {
        falha = new PerfilMobilidadeInvalidoException();
        mvc.perform(patch(URL).contentType("application/json").content(NOME))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Perfil de mobilidade inexistente ou inativo."));
    }

    @Test void rejeitaCorposInvalidos() throws Exception {
        mvc.perform(patch(URL).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(URL).contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test void usuarioAusenteRetorna404() throws Exception {
        falha = new EmptyResultDataAccessException(1);
        mvc.perform(get(URL)).andExpect(status().isNotFound());
        falha = new UsuarioNaoEncontradoException();
        mvc.perform(patch(URL).contentType("application/json").content(NOME)).andExpect(status().isNotFound());
    }

    @Test void falhaInternaNaoExpoeDetalhes() throws Exception {
        falha = new DataAccessResourceFailureException("segredo-do-banco");
        mvc.perform(get(URL)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Não foi possível concluir a operação."));
        mvc.perform(patch(URL).contentType("application/json").content(NOME))
                .andExpect(status().isInternalServerError());
    }

    @Test void exigeIdentidadeValida() throws Exception {
        principal = null;
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(patch(URL).contentType("application/json").content(NOME)).andExpect(status().isUnauthorized());
        mvc.perform(post(EXCLUSAO).contentType("application/json").content(SENHA)).andExpect(status().isUnauthorized());
        principal = Jwt.withTokenValue("teste").header("alg", "RS256").subject("invalido").build();
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test void exclusaoIndisponivelNoAuthRetorna503() throws Exception {
        falhaAuth = new com.maonaroda.api.exception.auth.FalhaAutenticacao(
                com.maonaroda.api.exception.auth.FalhaAutenticacao.Tipo.INDISPONIVEL);
        mvc.perform(post(EXCLUSAO).contentType("application/json").content(SENHA)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("INDISPONIVEL"));
    }

    @Test void exclusaoExigeSenhaENaoAceitaMaisDelete() throws Exception {
        mvc.perform(delete(URL)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(EXCLUSAO).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0]").value("senha"));
        assertNull(excluido);
    }

    @Test void senhaIncorretaRetorna403ENao401() throws Exception {
        falhaAuth = new com.maonaroda.api.exception.auth.FalhaAutenticacao(
                com.maonaroda.api.exception.auth.FalhaAutenticacao.Tipo.SENHA_INCORRETA);
        mvc.perform(post(EXCLUSAO).contentType("application/json").content(SENHA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Senha incorreta."));
    }

    @Test void tokenSemEmailNaoExclui() throws Exception {
        principal = Jwt.withTokenValue("teste").header("alg", "RS256").subject(ID.toString()).build();
        mvc.perform(post(EXCLUSAO).contentType("application/json").content(SENHA))
                .andExpect(status().isUnauthorized());
        assertNull(excluido);
    }
}
