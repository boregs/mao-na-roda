package com.maonaroda.mao_na_roda.data;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.function.LongSupplier;

/**
 * Sessão da conta (RNF16). O token é cifrado com uma chave do Android Keystore antes de ir
 * para o armazenamento local. Não há refresh token: ao expirar (15 min), é preciso entrar de novo.
 * Depois da expiração, apenas o e-mail fica guardado, como identificação visual (UC07 A3).
 */
public final class SessaoStore {
    /** Separa a criptografia para permitir testes na JVM, sem o Keystore. */
    public interface Cifra {
        String cifrar(String texto) throws Exception;
        String decifrar(String cifrado) throws Exception;
    }

    static final String ARQUIVO = "sessao";
    static final String TOKEN = "token";
    static final String EXPIRA_EM = "expira_em";
    static final String EMAIL = "ultimo_email";
    /** Considera expirado um pouco antes, para não enviar um token que vence no caminho. */
    static final long MARGEM_MS = 30_000;

    private static SessaoStore instancia;

    private final SharedPreferences preferencias;
    private final Cifra cifra;
    private final LongSupplier relogio;
    /** Usado quando o Keystore falha: a sessão vale só enquanto o processo existir. */
    private String tokenEmMemoria;

    public SessaoStore(SharedPreferences preferencias, Cifra cifra, LongSupplier relogio) {
        this.preferencias = preferencias;
        this.cifra = cifra;
        this.relogio = relogio;
    }

    public static synchronized SessaoStore de(Context context) {
        if (instancia == null) {
            Context app = context.getApplicationContext();
            instancia = new SessaoStore(app.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE),
                    new KeystoreCifra(), System::currentTimeMillis);
        }
        return instancia;
    }

    public synchronized void salvar(String token, long expiraEmSegundos, String email) {
        long expiraEm = relogio.getAsLong() + expiraEmSegundos * 1000;
        String cifrado = null;
        try {
            cifrado = cifra.cifrar(token);
            tokenEmMemoria = null;
        } catch (Exception erro) {
            tokenEmMemoria = token;
        }
        var editor = preferencias.edit().putLong(EXPIRA_EM, expiraEm).putString(EMAIL, email);
        if (cifrado == null) editor.remove(TOKEN); else editor.putString(TOKEN, cifrado);
        editor.apply();
    }

    /** Token ainda válido, ou null. Um token expirado é apagado na hora. */
    public synchronized String tokenValido() {
        if (relogio.getAsLong() >= preferencias.getLong(EXPIRA_EM, 0) - MARGEM_MS) {
            expirar();
            return null;
        }
        if (tokenEmMemoria != null) return tokenEmMemoria;
        String cifrado = preferencias.getString(TOKEN, null);
        if (cifrado == null) return null;
        try {
            return cifra.decifrar(cifrado);
        } catch (Exception erro) {
            expirar();
            return null;
        }
    }

    public boolean ativa() {
        return tokenValido() != null;
    }

    /** E-mail da última conta, mantido após a expiração apenas para identificação. */
    public synchronized String ultimoEmail() {
        return preferencias.getString(EMAIL, null);
    }

    /** Sessão expirada: remove o token e mantém o e-mail. */
    public synchronized void expirar() {
        tokenEmMemoria = null;
        preferencias.edit().remove(TOKEN).remove(EXPIRA_EM).apply();
    }

    /** Logout: remove tudo, inclusive o e-mail. */
    public synchronized void encerrar() {
        tokenEmMemoria = null;
        preferencias.edit().remove(TOKEN).remove(EXPIRA_EM).remove(EMAIL).apply();
    }
}
