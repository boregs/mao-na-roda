package com.maonaroda.api.rotas;

import com.maonaroda.api.config.SecurityConfig;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtValidationTests {
    static KeyPair pair;
    static NimbusJwtDecoder decoder;
    @BeforeAll static void preparar() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); pair = generator.generateKeyPair();
        decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).build();
        decoder.setJwtValidator(SecurityConfig.validadores("https://auth.example.test", "authenticated"));
    }
    private String token(String issuer, String audience, Instant inicio, Instant fim, KeyPair keys) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(UUID.randomUUID().toString())
                .issueTime(Date.from(inicio)).expirationTime(fim == null ? null : Date.from(fim)).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) keys.getPrivate())); return jwt.serialize();
    }
    @Test void aceitaAssinaturaEmissorAudienciaEValidadeCorretos() throws Exception {
        assertNotNull(decoder.decode(token("https://auth.example.test", "authenticated", Instant.now().minusSeconds(5), Instant.now().plusSeconds(600), pair)));
    }
    @Test void recusaExpiradoEmissorOuAudienciaIncorretos() throws Exception {
        var agora = Instant.now();
        String expired = token("https://auth.example.test", "authenticated", agora.minusSeconds(800), agora.minusSeconds(1), pair);
        String issuer = token("https://outro.example.test", "authenticated", agora, agora.plusSeconds(600), pair);
        String audience = token("https://auth.example.test", "outra", agora, agora.plusSeconds(600), pair);
        assertThrows(JwtException.class, () -> decoder.decode(expired));
        assertThrows(JwtException.class, () -> decoder.decode(issuer));
        assertThrows(JwtException.class, () -> decoder.decode(audience));
    }
    @Test void recusaValidadeAusenteOuSuperiorAQuinzeMinutos() throws Exception {
        var agora = Instant.now();
        String longa = token("https://auth.example.test", "authenticated", agora, agora.plusSeconds(3600), pair);
        String semPrazo = token("https://auth.example.test", "authenticated", agora, null, pair);
        assertThrows(JwtException.class, () -> decoder.decode(longa)); assertThrows(JwtException.class, () -> decoder.decode(semPrazo));
    }
    @Test void recusaAssinaturaDeOutraChave() throws Exception {
        var outra = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String invalido = token("https://auth.example.test", "authenticated", Instant.now(), Instant.now().plusSeconds(600), outra);
        assertThrows(JwtException.class, () -> decoder.decode(invalido));
    }
}
