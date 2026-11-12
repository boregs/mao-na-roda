package com.maonaroda.api.controller.auth;

import com.maonaroda.api.exception.auth.FalhaAutenticacao;
import com.maonaroda.api.exception.auth.FalhaAutenticacao.Tipo;
import com.maonaroda.api.model.CriarUsuarioRequest;
import com.maonaroda.api.model.LoginUsuarioRequest;
import com.maonaroda.api.model.auth.SessaoResponse;
import com.maonaroda.api.service.auth.AutenticacaoService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AutenticacaoControllerTests {
    static final String CADASTRO = "{\"nome\":\"Ana\",\"sobrenome\":\"Silva\","
            + "\"email\":\"ana@example.com\",\"senha\":\"senha12345\"}";
    static final String LOGIN = "{\"email\":\"ana@example.com\",\"senha\":\"senha12345\"}";
    static final SessaoResponse SESSAO = new SessaoResponse("jwt", "Bearer", 900);
    RuntimeException falha;
    Optional<SessaoResponse> cadastro;
    String tokenEncerrado;
    Jwt principal;
    MockMvc mvc;

    @BeforeEach void preparar() {
        cadastro = Optional.of(SESSAO);
        principal = Jwt.withTokenValue("jwt-atual").header("alg", "RS256").subject("x").build();
        var service = new AutenticacaoService(null, null) {
            @Override public Optional<SessaoResponse> cadastrar(CriarUsuarioRequest r) {
                if (falha != null) throw falha;
                return cadastro;
            }
            @Override public SessaoResponse entrar(LoginUsuarioRequest r) {
                if (falha != null) throw falha;
                return SESSAO;
            }
            @Override public void sair(String token) { tokenEncerrado = token; }
        };
        mvc = MockMvcBuilders.standaloneSetup(new AutenticacaoController(service))
                .setControllerAdvice(new AutenticacaoExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter p) { return p.getParameterType() == Jwt.class; }
                    @Override public Object resolveArgument(MethodParameter p, ModelAndViewContainer c,
                            NativeWebRequest r, WebDataBinderFactory f) { return principal; }
                }).build();
    }

    @Test void cadastroDevolveSessaoSemCache() throws Exception {
        mvc.perform(post("/api/v1/auth/cadastro").contentType("application/json").content(CADASTRO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiraEmSegundos").value(900))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test void cadastroPendenteDeConfirmacaoRetorna202() throws Exception {
        cadastro = Optional.empty();
        mvc.perform(post("/api/v1/auth/cadastro").contentType("application/json").content(CADASTRO))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test void validaCampos() throws Exception {
        mvc.perform(post("/api/v1/auth/cadastro").contentType("application/json")
                .content("{\"nome\":\" \",\"sobrenome\":\"S\",\"email\":\"x\",\"senha\":\"curta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0]").value("email"))
                .andExpect(jsonPath("$.campos[1]").value("nome"))
                .andExpect(jsonPath("$.campos[2]").value("senha"));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"email\":\"a@b.co\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test void loginDevolveSessao() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(LOGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accessToken").value("jwt"));
    }

    @Test void falhasViramRespostasPrevisiveis() throws Exception {
        assertFalha(Tipo.CREDENCIAIS_INVALIDAS, 401, "Não foi possível entrar. Confira o e-mail e a senha.");
        assertFalha(Tipo.CONTA_EXISTENTE, 409, "Já existe uma conta com este e-mail. Entre com sua senha.");
        assertFalha(Tipo.SENHA_FRACA, 400, "A senha não atende aos requisitos de segurança.");
        assertFalha(Tipo.EMAIL_NAO_CONFIRMADO, 403, "Confirme seu e-mail antes de entrar.");
        assertFalha(Tipo.INDISPONIVEL, 503, "O serviço de contas está indisponível. Tente novamente.");
    }

    @Test void bloqueioInformaQuandoTentarNovamente() throws Exception {
        falha = new FalhaAutenticacao(Tipo.BLOQUEADO, 840);
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "840"))
                .andExpect(jsonPath("$.codigo").value("BLOQUEADO"))
                .andExpect(jsonPath("$.tentarNovamenteEmSegundos").value(840));
    }

    @Test void bancoIndisponivelNaoExpoeDetalhes() throws Exception {
        falha = new DataAccessResourceFailureException("segredo-do-banco");
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(LOGIN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo"))));
    }

    @Test void logoutEncerraSessaoDoToken() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
        assertEquals("jwt-atual", tokenEncerrado);
        principal = null;
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
    }

    private void assertFalha(Tipo tipo, int status, String detalhe) throws Exception {
        falha = new FalhaAutenticacao(tipo);
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(LOGIN))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.detail").value(detalhe))
                .andExpect(jsonPath("$.codigo").value(tipo.name()));
    }
}
