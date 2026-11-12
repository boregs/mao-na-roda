package com.maonaroda.api.repository.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Contador do RNF18 por e-mail. Cada tentativa é reservada antes de consultar o Auth,
 * em um único comando atômico, para que tentativas simultâneas não ultrapassem o limite.
 * "tentativas" soma as falhas da janela e as tentativas ainda em andamento.
 */
@Repository
public class TentativasLoginRepository {
    public static final int LIMITE = 5;
    private static final String JANELA = "interval '15 minutes'";

    private final JdbcTemplate jdbc;

    public TentativasLoginRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Reserva uma tentativa. Falso quando o e-mail está bloqueado ou no limite. */
    public boolean reservar(String emailHash) {
        String expirada = "t.janela_inicio <= now() - " + JANELA;
        return !jdbc.queryForList(
                "INSERT INTO app.tentativas_login AS t (email_hash, tentativas, janela_inicio) "
                        + "VALUES (?, 1, now()) "
                        + "ON CONFLICT (email_hash) DO UPDATE SET "
                        + "tentativas = CASE WHEN " + expirada + " THEN 1 ELSE t.tentativas + 1 END, "
                        + "janela_inicio = CASE WHEN " + expirada + " THEN now() ELSE t.janela_inicio END, "
                        + "bloqueado_ate = NULL "
                        + "WHERE (t.bloqueado_ate IS NULL OR t.bloqueado_ate <= now()) "
                        + "AND (t.tentativas < " + LIMITE + " OR " + expirada + ") "
                        + "RETURNING t.tentativas",
                Integer.class, emailHash).isEmpty();
    }

    /** Segundos até o fim do bloqueio (ou da janela, se o limite foi atingido por tentativas em andamento). */
    public long segundosAteLiberar(String emailHash) {
        Long segundos = jdbc.query(
                "SELECT GREATEST(1, CEIL(EXTRACT(EPOCH FROM "
                        + "COALESCE(bloqueado_ate, janela_inicio + " + JANELA + ") - now())))::bigint "
                        + "FROM app.tentativas_login WHERE email_hash = ?",
                rs -> rs.next() ? rs.getLong(1) : null, emailHash);
        return segundos == null ? 1 : segundos;
    }

    /** Confirma a tentativa reservada como falha; a quinta falha da janela inicia o bloqueio. */
    public void registrarFalha(String emailHash) {
        jdbc.update("UPDATE app.tentativas_login SET bloqueado_ate = CASE WHEN tentativas >= " + LIMITE
                + " THEN now() + " + JANELA + " ELSE bloqueado_ate END WHERE email_hash = ?", emailHash);
    }

    /** Devolve a tentativa reservada quando o Auth falhou por outro motivo que não credenciais. */
    public void liberar(String emailHash) {
        jdbc.update("UPDATE app.tentativas_login SET tentativas = GREATEST(tentativas - 1, 0) "
                + "WHERE email_hash = ? AND bloqueado_ate IS NULL", emailHash);
    }

    /** Login bem-sucedido zera o contador. */
    public void limpar(String emailHash) {
        jdbc.update("DELETE FROM app.tentativas_login WHERE email_hash = ?", emailHash);
    }
}
