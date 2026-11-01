package com.maonaroda.api.repository.rota;

import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EvidenciaRepository {
    private final JdbcTemplate jdbc;
    public EvidenciaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Evidencia(String assinatura, String trecho, double inicio, double fim,
            String tipo, String classificacao, String condicao, String fonte, OffsetDateTime verificadoEm,
            String referenciaFonte, String versaoFonte, String motivoSemVerificacao) {
        public Evidencia(String assinatura, String trecho, double inicio, double fim, String tipo,
                String classificacao, String condicao, String fonte, OffsetDateTime verificadoEm) {
            this(assinatura, trecho, inicio, fim, tipo, classificacao, condicao, fonte, verificadoEm, null, null, null);
        }
    }
    public record Pacote(String versao, String matriz, List<Evidencia> evidencias) { }
    public Pacote carregar(String perfil) {
        // Uma instrução lê dataset e matriz na mesma visão MVCC, sem manter transação durante HTTP.
        final String[] versoes = {"SEM_DATASET", "RN-v1"};
        var evidencias = jdbc.query("""
                SELECT d.versao, d.versao_matriz, t.assinatura_rota, t.id::text AS trecho,
                    t.inicio, t.fim, b.codigo, c.classificacao, e.condicao, e.fonte, e.verificado_em,
                    e.id_fonte, e.versao_fonte, e.motivo_sem_verificacao
                FROM app.datasets_rotas d
                LEFT JOIN app.trechos_pedestres t ON t.dataset_id = d.id
                LEFT JOIN app.evidencias_trecho e ON e.trecho_id = t.id
                LEFT JOIN app.tipos_barreira b ON b.id = e.tipo_barreira_id AND b.ativo
                LEFT JOIN app.perfis_mobilidade p ON p.codigo = ? AND p.ativo
                LEFT JOIN app.classificacoes_barreira c ON c.tipo_barreira_id = b.id AND c.perfil_mobilidade_id = p.id
                WHERE d.ativo
                """, (rs, row) -> {
                    versoes[0] = rs.getString("versao"); versoes[1] = rs.getString("versao_matriz");
                    return new Evidencia(rs.getString("assinatura_rota"), rs.getString("trecho"),
                            rs.getDouble("inicio"), rs.getDouble("fim"), rs.getString("codigo"),
                            rs.getString("classificacao"), rs.getString("condicao"), rs.getString("fonte"),
                            rs.getObject("verificado_em", OffsetDateTime.class), rs.getString("id_fonte"),
                            rs.getString("versao_fonte"), rs.getString("motivo_sem_verificacao"));
                }, perfil);
        return new Pacote(versoes[0], versoes[1], evidencias.stream().filter(e -> e.trecho() != null).toList());
    }
}
