package com.maonaroda.mao_na_roda;

import static org.junit.Assert.*;

import com.maonaroda.mao_na_roda.data.ApiClient;
import com.maonaroda.mao_na_roda.data.ContaRepository;
import org.junit.Test;

public class ContaApiTest {
    @Test public void leSessaoSemRefreshToken() throws Exception {
        var sessao = ApiClient.lerSessao("{\"accessToken\":\"jwt\",\"tokenType\":\"Bearer\",\"expiraEmSegundos\":900}");
        assertEquals("jwt", sessao.accessToken());
        assertEquals(900, sessao.expiraEmSegundos());
    }

    @Test public void cadastroPendenteNaoTemSessao() throws Exception {
        assertNull(ApiClient.lerSessao("{\"status\":202,\"detail\":\"Conta criada. Confirme seu e-mail para entrar.\"}"));
    }

    @Test(expected = org.json.JSONException.class)
    public void sessaoInvalidaFalha() throws Exception {
        ApiClient.lerSessao("{\"accessToken\":\"\",\"expiraEmSegundos\":900}");
    }

    @Test public void bloqueioInformaQuandoTentarDeNovo() {
        assertEquals("Muitas tentativas de login. Tente novamente em 14 minutos.", ApiClient.mensagemConta(429,
                "{\"detail\":\"Muitas tentativas de login. Tente novamente mais tarde.\",\"codigo\":\"BLOQUEADO\",\"tentarNovamenteEmSegundos\":840}"));
        assertEquals("Muitas tentativas de login. Tente novamente em 1 minuto.", ApiClient.mensagemConta(429,
                "{\"codigo\":\"BLOQUEADO\",\"tentarNovamenteEmSegundos\":5}"));
    }

    @Test public void usaMensagemDaApi() {
        assertEquals("Já existe uma conta com este e-mail. Entre com sua senha.", ApiClient.mensagemConta(409,
                "{\"detail\":\"Já existe uma conta com este e-mail. Entre com sua senha.\",\"codigo\":\"CONTA_EXISTENTE\"}"));
        assertEquals("Não foi possível entrar. Confira o e-mail e a senha.", ApiClient.mensagemConta(401,
                "{\"detail\":\"Não foi possível entrar. Confira o e-mail e a senha.\"}"));
    }

    @Test public void semCorpoUsaMensagemPadrao() {
        assertEquals("Não foi possível entrar. Confira o e-mail e a senha.", ApiClient.mensagemConta(401, ""));
        assertEquals("O serviço de contas está indisponível. Tente novamente mais tarde.", ApiClient.mensagemConta(503, "<html>"));
        assertEquals("Não foi possível concluir. Tente novamente.", ApiClient.mensagemConta(500, "{\"detail\":\"interno\"}"));
    }

    @Test public void chamadaProtegidaIdentificaSessaoEncerrada() {
        assertEquals(ContaRepository.SESSAO_ENCERRADA, ApiClient.mensagemProtegida(401,
                "{\"codigo\":\"SESSAO_ENCERRADA\",\"detail\":\"Sua sessão terminou. Entre novamente.\"}"));
        // 503 da verificação de sessão é falha temporária, não fim da sessão.
        assertEquals("Não foi possível verificar a sessão. Tente novamente.", ApiClient.mensagemProtegida(503,
                "{\"codigo\":\"SESSAO_NAO_VERIFICADA\",\"detail\":\"Não foi possível verificar a sessão. Tente novamente.\"}"));
    }
}
