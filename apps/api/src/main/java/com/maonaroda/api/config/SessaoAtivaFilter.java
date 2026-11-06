package com.maonaroda.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RNF16: a assinatura e o prazo do JWT não mostram se a sessão terminou. Depois da
 * autenticação, confirma no Supabase Auth que a sessão do token continua ativa.
 * Sessão encerrada responde 401; banco indisponível responde 503, para que o aplicativo
 * não confunda uma falha temporária com o fim da sessão.
 * Registrado só na cadeia de segurança (SecurityConfig), não como filtro global.
 */
public class SessaoAtivaFilter extends OncePerRequestFilter {
    @FunctionalInterface
    public interface VerificadorSessao {
        boolean ativa(UUID sessao, UUID usuario);
    }

    private static final Logger log = LoggerFactory.getLogger(SessaoAtivaFilter.class);
    private final VerificadorSessao verificador;

    public SessaoAtivaFilter(VerificadorSessao verificador) {
        this.verificador = verificador;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            UUID sessao = uuid(jwt.getClaimAsString("session_id"));
            UUID usuario = uuid(jwt.getSubject());
            boolean ativa;
            try {
                ativa = sessao != null && usuario != null && verificador.ativa(sessao, usuario);
            } catch (DataAccessException erro) {
                log.warn("Não foi possível verificar a sessão: {}", erro.getClass().getSimpleName());
                responder(response, 503, "SESSAO_NAO_VERIFICADA",
                        "Não foi possível verificar a sessão. Tente novamente.");
                return;
            }
            if (!ativa) {
                SecurityContextHolder.clearContext();
                response.setHeader("WWW-Authenticate",
                        "Bearer error=\"invalid_token\", error_description=\"Sessão encerrada\"");
                responder(response, 401, "SESSAO_ENCERRADA", "Sua sessão terminou. Entre novamente.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static UUID uuid(String valor) {
        if (valor == null) return null;
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException erro) {
            return null;
        }
    }

    private static void responder(HttpServletResponse response, int status, String codigo, String detalhe)
            throws IOException {
        response.setStatus(status);
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"codigo\":\"" + codigo
                + "\",\"detail\":\"" + detalhe + "\"}");
    }
}
