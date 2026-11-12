package com.maonaroda.mao_na_roda;

import static org.junit.Assert.*;

import com.maonaroda.mao_na_roda.data.SessaoStore;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;

public class SessaoStoreTest {
    private final AtomicLong agora = new AtomicLong(1_000_000);
    private AccessibilityProfileStoreTest.FakePreferences preferencias;
    private boolean cifraFalha;
    private SessaoStore store;

    @Before public void preparar() {
        preferencias = new AccessibilityProfileStoreTest.FakePreferences();
        store = new SessaoStore(preferencias, new SessaoStore.Cifra() {
            @Override public String cifrar(String texto) throws Exception {
                if (cifraFalha) throw new Exception("keystore");
                return "cifrado:" + new StringBuilder(texto).reverse();
            }
            @Override public String decifrar(String cifrado) throws Exception {
                if (cifraFalha) throw new Exception("keystore");
                return new StringBuilder(cifrado.substring("cifrado:".length())).reverse().toString();
            }
        }, agora::get);
    }

    @Test public void tokenNaoFicaLegivelNoArmazenamento() {
        store.salvar("jwt-secreto", 900, "ana@example.com");
        assertEquals("jwt-secreto", store.tokenValido());
        assertTrue(store.ativa());
        for (Object valor : preferencias.getAll().values())
            assertFalse(String.valueOf(valor).contains("jwt-secreto"));
    }

    @Test public void expiracaoApagaTokenEMantemEmailParaIdentificacao() {
        store.salvar("jwt", 900, "ana@example.com");
        agora.addAndGet(900_000 - 30_000);
        assertNull("expira com margem", store.tokenValido());
        assertFalse(preferencias.contains("token"));
        assertEquals("ana@example.com", store.ultimoEmail());
    }

    @Test public void logoutApagaTudo() {
        store.salvar("jwt", 900, "ana@example.com");
        store.encerrar();
        assertNull(store.tokenValido());
        assertNull(store.ultimoEmail());
        assertTrue(preferencias.getAll().isEmpty());
    }

    @Test public void semKeystoreSessaoValeSoNaMemoria() {
        cifraFalha = true;
        store.salvar("jwt", 900, "ana@example.com");
        assertFalse(preferencias.contains("token"));
        assertEquals("jwt", store.tokenValido());
    }

    @Test public void tokenCorrompidoEncerraSessao() {
        store.salvar("jwt", 900, "ana@example.com");
        cifraFalha = true;
        assertNull(store.tokenValido());
        assertEquals("ana@example.com", store.ultimoEmail());
    }

    @Test public void semSessaoNadaEhDevolvido() {
        assertNull(store.tokenValido());
        assertNull(store.ultimoEmail());
    }
}
