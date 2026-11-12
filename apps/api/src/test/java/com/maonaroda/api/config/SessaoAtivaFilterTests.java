package com.maonaroda.api.config;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;

class SessaoAtivaFilterTests {
    final UUID usuario = UUID.randomUUID(), sessao = UUID.randomUUID();
    final AtomicInteger consultas = new AtomicInteger();

    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    private MockHttpServletResponse executar(SessaoAtivaFilter.VerificadorSessao verificador, Object sessaoClaim,
            MockFilterChain chain) throws Exception {
        if (sessaoClaim != null || chain != null) {
            var builder = Jwt.withTokenValue("t").header("alg", "RS256").subject(usuario.toString());
            if (sessaoClaim != null) builder.claim("session_id", sessaoClaim.toString());
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(builder.build()));
        }
        var response = new MockHttpServletResponse();
        new SessaoAtivaFilter((s, u) -> { consultas.incrementAndGet(); return verificador.ativa(s, u); })
                .doFilter(new MockHttpServletRequest("GET", "/api/v1/usuarios/me"), response,
                        chain == null ? new MockFilterChain() : chain);
        return response;
    }

    @Test void sessaoAtivaSegue() throws Exception {
        var chain = new MockFilterChain();
        var response = executar((s, u) -> s.equals(sessao) && u.equals(usuario), sessao, chain);
        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest(), "a requisição deve seguir para o controller");
    }

    @Test void sessaoEncerradaResponde401() throws Exception {
        var chain = new MockFilterChain();
        var response = executar((s, u) -> false, sessao, chain);
        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        assertTrue(response.getHeader("WWW-Authenticate").contains("invalid_token"));
        assertTrue(response.getContentAsString().contains("SESSAO_ENCERRADA"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void claimAusenteOuInvalidaResponde401SemConsultarBanco() throws Exception {
        var semClaim = new MockFilterChain();
        assertEquals(401, executar((s, u) -> true, null, semClaim).getStatus());
        assertEquals(401, executar((s, u) -> true, "nao-e-uuid", new MockFilterChain()).getStatus());
        assertEquals(0, consultas.get());
    }

    @Test void bancoIndisponivelResponde503ENao401() throws Exception {
        var response = executar((s, u) -> { throw new DataAccessResourceFailureException("segredo"); }, sessao,
                new MockFilterChain());
        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains("SESSAO_NAO_VERIFICADA"));
        assertFalse(response.getContentAsString().contains("segredo"));
    }

    @Test void requisicaoSemTokenNaoConsultaSessao() throws Exception {
        var chain = new MockFilterChain();
        var response = new MockHttpServletResponse();
        new SessaoAtivaFilter((s, u) -> { consultas.incrementAndGet(); return false; })
                .doFilter(new MockHttpServletRequest("POST", "/api/v1/rotas"), response, chain);
        assertNotNull(chain.getRequest());
        assertEquals(0, consultas.get());
    }
}
