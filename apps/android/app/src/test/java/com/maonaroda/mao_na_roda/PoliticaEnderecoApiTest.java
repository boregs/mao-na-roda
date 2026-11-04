package com.maonaroda.mao_na_roda;

import com.maonaroda.mao_na_roda.data.PoliticaEnderecoApi;
import org.junit.Test;
import static org.junit.Assert.*;

public class PoliticaEnderecoApiTest {
    @Test public void httpLocalSoEPermitidoEmDepuracao() {
        for (String host : new String[]{"10.0.2.2", "127.0.0.1", "localhost"}) {
            String url = "http://" + host + ":8080";
            assertTrue(url, PoliticaEnderecoApi.permitida(url, true));
            assertFalse(url, PoliticaEnderecoApi.permitida(url, false));
        }
        assertTrue(PoliticaEnderecoApi.permitida("https://api.example.com", false));
    }
    @Test public void rejeitaHttpExternoEEnderecosAmbiguos() {
        for (String url : new String[]{"", "not a url", "http://192.168.1.2:8080", "http://api.example.com",
                "http://localhost.example.com", "http://127.0.0.2", "http://localhost@api.example.com",
                "https://usuario:senha@api.example.com", "https://api.example.com?chave=segredo",
                "https://api.example.com#fragmento", "file:///localhost/api"}) {
            assertFalse(url, PoliticaEnderecoApi.permitida(url, true));
        }
    }
}
