package com.maonaroda.api.service.auth;

import com.maonaroda.api.exception.auth.FalhaAutenticacao;
import com.maonaroda.api.exception.auth.FalhaAutenticacao.Tipo;
import com.maonaroda.api.integracao.supabase.SupabaseAuthClient;
import com.maonaroda.api.model.CriarUsuarioRequest;
import com.maonaroda.api.model.LoginUsuarioRequest;
import com.maonaroda.api.model.auth.SessaoResponse;
import com.maonaroda.api.repository.auth.TentativasLoginRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutenticacaoServiceTests {
    static final SessaoResponse SESSAO = new SessaoResponse("jwt", "Bearer", 900);
    final List<String> eventos = new ArrayList<>();
    RuntimeException falhaLogin;
    boolean permitir;
    String emailEnviado, nomeEnviado;
    AutenticacaoService service;

    @BeforeEach void preparar() {
        permitir = true;
        var client = new SupabaseAuthClient("", "", Duration.ofSeconds(1)) {
            @Override public Optional<SessaoResponse> cadastrar(String email, String senha, String nome, String sobrenome) {
                emailEnviado = email; nomeEnviado = nome + "|" + sobrenome;
                return Optional.of(SESSAO);
            }
            @Override public SessaoResponse entrar(String email, String senha) {
                emailEnviado = email;
                eventos.add("auth");
                if (falhaLogin != null) throw falhaLogin;
                return SESSAO;
            }
        };
        var repo = new TentativasLoginRepository(null) {
            @Override public boolean reservar(String hash) { eventos.add("reservar:" + hash.length()); return permitir; }
            @Override public long segundosAteLiberar(String hash) { return 600; }
            @Override public void registrarFalha(String hash) { eventos.add("falha"); }
            @Override public void liberar(String hash) { eventos.add("liberar"); }
            @Override public void limpar(String hash) { eventos.add("limpar"); }
        };
        service = new AutenticacaoService(client, repo);
    }

    @Test void cadastroNormalizaEmailENomes() {
        assertEquals(SESSAO, service.cadastrar(new CriarUsuarioRequest(" Ana ", " Silva ", " Ana@Example.COM ", "senha12345")).orElseThrow());
        assertEquals("ana@example.com", emailEnviado);
        assertEquals("Ana|Silva", nomeEnviado);
    }

    @Test void loginBemSucedidoZeraContador() {
        assertEquals(SESSAO, service.entrar(new LoginUsuarioRequest("Ana@Example.com", "senha")));
        assertEquals(List.of("reservar:64", "auth", "limpar"), eventos);
        assertEquals("ana@example.com", emailEnviado);
    }

    @Test void credencialInvalidaConfirmaFalha() {
        falhaLogin = new FalhaAutenticacao(Tipo.CREDENCIAIS_INVALIDAS);
        var erro = assertThrows(FalhaAutenticacao.class, () -> service.entrar(new LoginUsuarioRequest("a@b.co", "x")));
        assertEquals(Tipo.CREDENCIAIS_INVALIDAS, erro.tipo());
        assertEquals(List.of("reservar:64", "auth", "falha"), eventos);
    }

    @Test void indisponibilidadeNaoContaComoFalha() {
        falhaLogin = new FalhaAutenticacao(Tipo.INDISPONIVEL);
        assertThrows(FalhaAutenticacao.class, () -> service.entrar(new LoginUsuarioRequest("a@b.co", "x")));
        assertEquals(List.of("reservar:64", "auth", "liberar"), eventos);
        eventos.clear();
        falhaLogin = new IllegalStateException("inesperado");
        assertThrows(IllegalStateException.class, () -> service.entrar(new LoginUsuarioRequest("a@b.co", "x")));
        assertEquals(List.of("reservar:64", "auth", "liberar"), eventos);
    }

    @Test void bloqueadoNaoConsultaAuth() {
        permitir = false;
        var erro = assertThrows(FalhaAutenticacao.class, () -> service.entrar(new LoginUsuarioRequest("a@b.co", "x")));
        assertEquals(Tipo.BLOQUEADO, erro.tipo());
        assertEquals(600, erro.tentarNovamenteEmSegundos());
        assertEquals(List.of("reservar:64"), eventos);
    }

    @Test void hashIgnoraCaixaEEspacos() {
        assertEquals(AutenticacaoService.hash(AutenticacaoService.normalizar(" A@B.co ")),
                AutenticacaoService.hash(AutenticacaoService.normalizar("a@b.CO")));
        assertTrue(AutenticacaoService.hash("a@b.co").matches("[0-9a-f]{64}"));
    }
}
