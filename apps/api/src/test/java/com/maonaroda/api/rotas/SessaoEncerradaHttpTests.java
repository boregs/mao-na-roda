package com.maonaroda.api.rotas;

import com.maonaroda.api.config.SecurityConfig;
import com.maonaroda.api.config.SessaoAtivaFilter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import static org.junit.jupiter.api.Assertions.*;

/** Cadeia de segurança completa: token assinado e válido, mas com a sessão encerrada. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessaoEncerradaHttpTests {
    static final String EMISSOR = "https://auth.example.test";
    static final KeyPair CHAVES = gerar();
    /** "ativa", "encerrada" ou "falha" para simular o banco. */
    static final AtomicReference<String> estado = new AtomicReference<>("ativa");

    @TestConfiguration
    static class Configuracao {
        @Bean @Primary JwtDecoder decodificadorDeTeste() {
            var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) CHAVES.getPublic()).build();
            decoder.setJwtValidator(SecurityConfig.validadores(EMISSOR, "authenticated"));
            return decoder;
        }
        @Bean @Primary SessaoAtivaFilter.VerificadorSessao verificadorDeTeste() {
            return (sessao, usuario) -> switch (estado.get()) {
                case "falha" -> throw new DataAccessResourceFailureException("banco");
                case "encerrada" -> false;
                default -> true;
            };
        }
    }

    @LocalServerPort int port;

    static KeyPair gerar() {
        try {
            var gerador = KeyPairGenerator.getInstance("RSA");
            gerador.initialize(2048);
            return gerador.generateKeyPair();
        } catch (Exception erro) { throw new IllegalStateException(erro); }
    }

    static String token() throws Exception {
        var agora = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(EMISSOR).audience("authenticated")
                .subject(UUID.randomUUID().toString()).claim("session_id", UUID.randomUUID().toString())
                .issueTime(Date.from(agora)).expirationTime(Date.from(agora.plusSeconds(600))).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) CHAVES.getPrivate()));
        return jwt.serialize();
    }

    HttpResponse<String> logout() throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/logout"))
                    .header("Authorization", "Bearer " + token())
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test void sessaoEncerradaRecusaTokenValido() throws Exception {
        estado.set("encerrada");
        var resposta = logout();
        assertEquals(401, resposta.statusCode());
        assertTrue(resposta.body().contains("SESSAO_ENCERRADA"), resposta.body());
    }

    @Test void sessaoAtivaChegaAoController() throws Exception {
        estado.set("ativa");
        var resposta = logout();
        // O Supabase não está configurado nos testes: o controller responde indisponível.
        assertEquals(503, resposta.statusCode());
        assertTrue(resposta.body().contains("INDISPONIVEL"), resposta.body());
    }

    @Test void bancoIndisponivelNaoEncerraSessao() throws Exception {
        estado.set("falha");
        var resposta = logout();
        assertEquals(503, resposta.statusCode());
        assertTrue(resposta.body().contains("SESSAO_NAO_VERIFICADA"), resposta.body());
    }
}
