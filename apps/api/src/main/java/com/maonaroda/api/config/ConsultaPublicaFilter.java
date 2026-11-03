package com.maonaroda.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ConsultaPublicaFilter extends OncePerRequestFilter {
    private record Janela(long inicio, int consultas) { }
    private final Map<String, Janela> janelas = new HashMap<>();
    private synchronized boolean permitir(String endereco) {
        long agora = System.nanoTime();
        janelas.entrySet().removeIf(e -> agora - e.getValue().inicio() >= 60_000_000_000L);
        var atual = janelas.get(endereco);
        if (atual == null && janelas.size() >= 2048) return false;
        if (atual != null && atual.consultas() >= 30) return false;
        janelas.put(endereco, new Janela(atual == null ? agora : atual.inicio(), atual == null ? 1 : atual.consultas() + 1));
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/api/v1/rotas") || path.startsWith("/api/v1/destinos")) {
            response.setHeader("Cache-Control", "no-store");

            if ("POST".equals(request.getMethod()) && !permitir(request.getRemoteAddr())) {
                response.setStatus(429); response.setHeader("Retry-After", "60");
                response.setContentType("application/problem+json"); response.setCharacterEncoding("UTF-8");

                response.getWriter().write("{\"status\":429,\"title\":\"Limite de consultas\",\"detail\":\"Aguarde um minuto antes de consultar novamente.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
