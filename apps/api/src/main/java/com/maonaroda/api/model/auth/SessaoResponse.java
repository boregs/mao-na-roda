package com.maonaroda.api.model.auth;

/**
 * Sessão entregue ao aplicativo. O refresh token do Supabase nunca sai da API:
 * ao expirar (15 min, RNF16) o aplicativo precisa de um novo login.
 */
public record SessaoResponse(String accessToken, String tokenType, long expiraEmSegundos) {
}
