package com.maonaroda.api.service.auth;

import com.maonaroda.api.integracao.supabase.SupabaseAuthClient;
import com.maonaroda.api.model.CriarUsuarioRequest;
import com.maonaroda.api.model.LoginUsuarioRequest;
import com.maonaroda.api.model.auth.SessaoResponse;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutenticacaoServiceTests {
    static final SessaoResponse SESSAO = new SessaoResponse("jwt", "Bearer", 900);
    String emailEnviado, nomeEnviado;
    AutenticacaoService service;

    @BeforeEach void preparar() {
        var client = new SupabaseAuthClient("", "", Duration.ofSeconds(1)) {
            @Override public Optional<SessaoResponse> cadastrar(String email, String senha, String nome, String sobrenome) {
                emailEnviado = email; nomeEnviado = nome + "|" + sobrenome;
                return Optional.of(SESSAO);
            }
            @Override public SessaoResponse entrar(String email, String senha) {
                emailEnviado = email;
                return SESSAO;
            }
        };
        service = new AutenticacaoService(client);
    }

    @Test void cadastroNormalizaEmailENomes() {
        assertEquals(SESSAO, service.cadastrar(new CriarUsuarioRequest(" Ana ", " Silva ", " Ana@Example.COM ", "senha12345")).orElseThrow());
        assertEquals("ana@example.com", emailEnviado);
        assertEquals("Ana|Silva", nomeEnviado);
    }

    @Test void loginNormalizaEmail() {
        assertEquals(SESSAO, service.entrar(new LoginUsuarioRequest(" Ana@Example.com ", "senha")));
        assertEquals("ana@example.com", emailEnviado);
    }
}
