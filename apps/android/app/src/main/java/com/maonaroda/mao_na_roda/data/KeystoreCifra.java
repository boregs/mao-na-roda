package com.maonaroda.mao_na_roda.data;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** AES-GCM com chave não exportável do Android Keystore (API 23+). */
final class KeystoreCifra implements SessaoStore.Cifra {
    private static final String PROVEDOR = "AndroidKeyStore";
    private static final String APELIDO = "mao_na_roda_sessao";
    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";

    private static SecretKey chave() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PROVEDOR);
        keyStore.load(null);
        if (keyStore.containsAlias(APELIDO)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(APELIDO, null)).getSecretKey();
        }
        KeyGenerator gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEDOR);
        gerador.init(new KeyGenParameterSpec.Builder(APELIDO,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return gerador.generateKey();
    }

    @Override public String cifrar(String texto) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
        cipher.init(Cipher.ENCRYPT_MODE, chave());
        byte[] dados = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(dados, Base64.NO_WRAP);
    }

    @Override public String decifrar(String cifrado) throws Exception {
        String[] partes = cifrado.split(":", 2);
        if (partes.length != 2) throw new IllegalArgumentException();
        Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
        cipher.init(Cipher.DECRYPT_MODE, chave(),
                new GCMParameterSpec(128, Base64.decode(partes[0], Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(partes[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
}
