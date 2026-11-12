package com.maonaroda.api.integracao.supabase;

import com.maonaroda.api.exception.auth.FalhaAutenticacao;
import com.maonaroda.api.exception.auth.FalhaAutenticacao.Tipo;
import com.maonaroda.api.model.auth.SessaoResponse;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador da API REST do Supabase Auth (GoTrue). A chave anon fica somente no servidor;
 * o aplicativo nunca fala diretamente com o Auth, para que o limite do RNF18 não seja contornado.
 */
@Component
public class SupabaseAuthClient {
    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthClient.class);

    private final String url;
    private final String anonKey;
    /** Chave de serviço (service_role ou sb_secret_…): ignora RLS; usada só na exclusão de contas. */
    private final String serviceKey;
    private final Duration timeout;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public SupabaseAuthClient(String url, String anonKey, Duration timeout) {
        this(url, anonKey, "", timeout);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SupabaseAuthClient(@Value("${supabase.url:}") String url,
            @Value("${supabase.anon-key:}") String anonKey,
            @Value("${supabase.service-role-key:}") String serviceKey,
            @Value("${supabase.auth.timeout:5s}") Duration timeout) {
        this.url = url.replaceAll("/+$", "");
        this.anonKey = anonKey;
        this.serviceKey = serviceKey;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @PreDestroy
    public void close() {
        http.close();
    }

    /** Cria a conta. Vazio quando o projeto exige confirmação de e-mail (sem sessão ainda). */
    public Optional<SessaoResponse> cadastrar(String email, String senha, String nome, String sobrenome) {
        var corpo = new LinkedHashMap<String, Object>();
        corpo.put("email", email);
        corpo.put("password", senha);
        corpo.put("data", Map.of("nome", nome, "sobrenome", sobrenome));
        var resposta = enviar("POST", "/auth/v1/signup", corpo, null, anonKey);
        if (!sucesso(resposta.status())) throw erroCadastro(resposta);
        if (!resposta.corpo().hasNonNull("access_token")) return Optional.empty();
        return Optional.of(sessao(resposta.corpo()));
    }

    public SessaoResponse entrar(String email, String senha) {
        var resposta = enviar("POST", "/auth/v1/token?grant_type=password",
                Map.of("email", email, "password", senha), null, anonKey);
        if (sucesso(resposta.status())) return sessao(resposta.corpo());
        throw erroLogin(resposta);
    }

    private FalhaAutenticacao erroLogin(Resposta resposta) {
        String codigo = codigo(resposta.corpo());
        if ("email_not_confirmed".equals(codigo)) return new FalhaAutenticacao(Tipo.EMAIL_NAO_CONFIRMADO);
        if (resposta.status() == 400 && ("invalid_credentials".equals(codigo) || "invalid_grant".equals(codigo)))
            return new FalhaAutenticacao(Tipo.CREDENCIAIS_INVALIDAS);
        return erroGenerico(resposta);
    }

    /**
     * Confirma a senha (UC10 A2) e devolve o usuário dono dela. A confirmação abre uma sessão
     * no Auth; ela é encerrada em seguida para não ficar ativa sem uso.
     */
    public java.util.UUID confirmarSenha(String email, String senha) {
        var resposta = enviar("POST", "/auth/v1/token?grant_type=password",
                Map.of("email", email, "password", senha), null, anonKey);
        if (!sucesso(resposta.status())) throw erroLogin(resposta);
        String token = resposta.corpo().path("access_token").asString("");
        java.util.UUID usuario;
        try {
            usuario = java.util.UUID.fromString(resposta.corpo().path("user").path("id").asString(""));
        } catch (IllegalArgumentException erro) {
            log.warn("Supabase Auth devolveu login sem identificador de usuário.");
            throw new FalhaAutenticacao(Tipo.INDISPONIVEL);
        }
        if (!token.isBlank()) {
            try {
                sair(token);
            } catch (FalhaAutenticacao erro) {
                log.warn("Não foi possível encerrar a sessão de confirmação de senha.");
            }
        }
        return usuario;
    }

    /** Encerra somente a sessão do token informado. Sessão já encerrada não é erro. */
    public void sair(String accessToken) {
        var resposta = enviar("POST", "/auth/v1/logout?scope=local", null, accessToken, anonKey);
        int status = resposta.status();
        if (sucesso(status) || status == 401 || status == 403 || status == 404) return;
        throw erroGenerico(resposta);
    }

    /**
     * Remove a identidade no Auth. O banco apaga em cascata as sessões e app.usuarios,
     * e o trigger da V3 desvincula os relatos (RN16). Usuário já removido não é erro.
     */
    public void excluirUsuario(java.util.UUID usuario) {
        if (serviceKey.isBlank()) {
            log.warn("Exclusão de contas não configurada (supabase.service-role-key).");
            throw new FalhaAutenticacao(Tipo.INDISPONIVEL);
        }
        // As chaves antigas (JWT) também vão no Authorization; as novas sb_secret_ só no apikey.
        String bearer = serviceKey.startsWith("eyJ") ? serviceKey : null;
        var resposta = enviar("DELETE", "/auth/v1/admin/users/" + usuario, null, bearer, serviceKey);
        if (sucesso(resposta.status()) || resposta.status() == 404) return;
        throw erroGenerico(resposta);
    }

    private FalhaAutenticacao erroCadastro(Resposta resposta) {
        String codigo = codigo(resposta.corpo());
        return switch (codigo) {
            case "user_already_exists", "email_exists" -> new FalhaAutenticacao(Tipo.CONTA_EXISTENTE);
            case "weak_password" -> new FalhaAutenticacao(Tipo.SENHA_FRACA);
            case "email_address_invalid", "email_address_not_authorized", "validation_failed" ->
                    new FalhaAutenticacao(Tipo.EMAIL_INVALIDO);
            default -> erroGenerico(resposta);
        };
    }

    private FalhaAutenticacao erroGenerico(Resposta resposta) {
        if (resposta.status() == 429) return new FalhaAutenticacao(Tipo.LIMITE_PROVEDOR, 60);
        // Somente o código técnico vai para o log; nunca e-mail, senha ou token.
        log.warn("Supabase Auth respondeu HTTP {} ({})", resposta.status(), codigo(resposta.corpo()));
        return new FalhaAutenticacao(Tipo.INDISPONIVEL);
    }

    private SessaoResponse sessao(JsonNode corpo) {
        String token = corpo.path("access_token").asString("");
        long expira = corpo.path("expires_in").asLong(0);
        if (token.isBlank() || expira <= 0) {
            log.warn("Supabase Auth devolveu sessão sem token ou validade.");
            throw new FalhaAutenticacao(Tipo.INDISPONIVEL);
        }
        return new SessaoResponse(token, "Bearer", expira);
    }

    private static String codigo(JsonNode corpo) {
        for (String campo : new String[] {"error_code", "error"}) {
            String valor = corpo.path(campo).asString("");
            if (!valor.isBlank()) return valor;
        }
        return "";
    }

    private static boolean sucesso(int status) {
        return status >= 200 && status < 300;
    }

    private record Resposta(int status, JsonNode corpo) { }

    private Resposta enviar(String metodo, String caminho, Object corpo, String bearer, String chave) {
        if (url.isBlank() || chave.isBlank()) {
            log.warn("Supabase Auth não configurado (supabase.url / supabase.anon-key).");
            throw new FalhaAutenticacao(Tipo.INDISPONIVEL);
        }
        try {
            var builder = HttpRequest.newBuilder(URI.create(url + caminho))
                    .timeout(timeout)
                    .header("apikey", chave)
                    .header("Accept", "application/json");
            if (bearer != null) builder.header("Authorization", "Bearer " + bearer);
            if (corpo == null) {
                builder.method(metodo, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(metodo, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)));
            }
            var resposta = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode node;
            try {
                String texto = resposta.body();
                node = texto == null || texto.isBlank() ? json.createObjectNode() : json.readTree(texto);
            } catch (RuntimeException erro) {
                node = json.createObjectNode();
            }
            return new Resposta(resposta.statusCode(), node);
        } catch (IOException erro) {
            log.warn("Falha de rede com o Supabase Auth: {}", erro.getClass().getSimpleName());
            throw new FalhaAutenticacao(Tipo.INDISPONIVEL);
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new FalhaAutenticacao(Tipo.INDISPONIVEL);
        }
    }
}
