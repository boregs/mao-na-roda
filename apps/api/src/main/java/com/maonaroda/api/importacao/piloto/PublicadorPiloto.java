package com.maonaroda.api.importacao.piloto;

import com.maonaroda.api.integracao.rotas.CandidataRota;
import com.maonaroda.api.model.rota.ConsultaRotasResponse.Geometria;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.time.OffsetDateTime;
import java.util.*;

/** Importação administrativa; não é exposta por HTTP nem usa o papel maonaroda_api. */
@Service
public class PublicadorPiloto {
    public record Dataset(String versao, List<RotaRevisada> rotas) { }
    public record RotaRevisada(String assinatura, Geometria geometria, List<Trecho> trechos) { }
    public record Trecho(double inicio, double fim, String lado, Integer nivel, Boolean travessia, Boolean entrada, OffsetDateTime contextoRevisadoEm, List<Evidencia> evidencias) { }
    public record Evidencia(String tipo, String condicao, String fonte, String idFonte, String versaoFonte, String lote, OffsetDateTime dataOriginal, OffsetDateTime verificadoEm, String motivoSemVerificacao) { }
    private final JdbcTemplate jdbc;

    public PublicadorPiloto(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static String obrigatorio(String value) {
        if (value == null || value.isBlank() || value.length() > 500) throw new IllegalArgumentException("Campo de revisão obrigatório ou excessivo.");
        return value;
    }

    @Transactional
    public UUID publicar(Dataset dataset) {
        if (dataset == null || dataset.rotas() == null || dataset.rotas().isEmpty() || dataset.rotas().size() > 300)
            throw new IllegalArgumentException("Informe de 1 a 300 geometrias revisadas.");

        obrigatorio(dataset.versao());

        var tipos = new HashSet<>(jdbc.queryForList("SELECT codigo FROM app.tipos_barreira WHERE ativo", String.class));

        if (tipos.size() != 10)
            throw new IllegalArgumentException("Catálogo incompatível com a matriz RN-v1.");

        jdbc.execute("SELECT pg_advisory_xact_lock(4261101)");

        UUID id = UUID.randomUUID();

        jdbc.update("INSERT INTO app.datasets_rotas(id, versao, versao_matriz) VALUES (?, ?, 'RN-v1')", id, dataset.versao());

        var assinaturas = new HashSet<String>();
        var agora = OffsetDateTime.now();
        var json = JsonMapper.builder().build();

        for (var rota : dataset.rotas()) {
            if (rota.geometria() == null ||
                    !"LineString".equals(rota.geometria().type()) ||
                    rota.trechos() == null || rota.trechos().isEmpty() ||
                    rota.trechos().size() > 100)
                throw new IllegalArgumentException("Geometria e trechos revisados são obrigatórios.");


            String assinatura = new CandidataRota(rota.geometria().coordinates(), 1, 1).assinatura();

            if (!assinatura.equals(rota.assinatura()) || !assinaturas.add(assinatura))
                throw new IllegalArgumentException("Assinatura divergente da geometria ou duplicada.");

            double fimAnterior = 0;
            for (var trecho : rota.trechos()) {
                if (!Double.isFinite(trecho.inicio()) || !Double.isFinite(trecho.fim()) || trecho.inicio() < fimAnterior
                        || trecho.inicio() < 0 || trecho.fim() > 1 || trecho.fim() <= trecho.inicio()
                        || trecho.nivel() == null || trecho.travessia() == null || trecho.entrada() == null
                        || trecho.contextoRevisadoEm() == null || trecho.contextoRevisadoEm().isAfter(agora))
                    throw new IllegalArgumentException("Contexto não revisado, intervalos inválidos ou sobrepostos.");


                obrigatorio(trecho.lado()); fimAnterior = trecho.fim();

                if (trecho.evidencias() == null || trecho.evidencias().size() != 10)
                    throw new IllegalArgumentException("Registre as dez condições; use desconhecida quando faltar observação.");

                UUID trechoId = UUID.randomUUID();

                jdbc.update("""
                    INSERT INTO app.trechos_pedestres(id, dataset_id, assinatura_rota, inicio, fim, geometria,
                        lado, nivel, travessia, entrada, contexto_revisado_em)
                    VALUES (?, ?, ?, ?, ?, extensions.ST_LineSubstring(extensions.ST_SetSRID(extensions.ST_GeomFromGeoJSON(?),4326), ?, ?), ?, ?, ?, ?, ?)
                    """, trechoId, id, assinatura, trecho.inicio(), trecho.fim(), json.writeValueAsString(rota.geometria()),
                        trecho.inicio(), trecho.fim(), trecho.lado(), trecho.nivel(), trecho.travessia(), trecho.entrada(), trecho.contextoRevisadoEm());

                var vistos = new HashSet<String>();

                for (var e : trecho.evidencias()) {
                    if (!tipos.contains(e.tipo()) || !vistos.add(e.tipo()) || !Set.of("presente", "ausente", "desconhecida").contains(e.condicao() == null ? "" : e.condicao())
                            || !Set.of("openstreetmap", "dados_publicos").contains(e.fonte() == null ? "" : e.fonte())
                            || (e.verificadoEm() != null && e.verificadoEm().isAfter(agora))
                            || (e.verificadoEm() == null && (e.motivoSemVerificacao() == null || e.motivoSemVerificacao().isBlank())))
                        throw new IllegalArgumentException("Evidência inválida ou motivo de data desconhecida ausente.");

                    obrigatorio(e.idFonte()); obrigatorio(e.versaoFonte()); obrigatorio(e.lote());

                    jdbc.update("""
                        INSERT INTO app.evidencias_trecho(trecho_id, tipo_barreira_id, condicao, fonte, id_fonte,
                            versao_fonte, lote, data_original, verificado_em, motivo_sem_verificacao)
                        SELECT ?, id, ?, ?, ?, ?, ?, ?, ?, ? FROM app.tipos_barreira WHERE codigo = ? AND ativo
                        """, trechoId, e.condicao(), e.fonte(), e.idFonte(), e.versaoFonte(), e.lote(),
                            e.dataOriginal(), e.verificadoEm(), e.motivoSemVerificacao(), e.tipo());
                }
            }
        }

        jdbc.update("UPDATE app.datasets_rotas SET ativo = false WHERE ativo");
        jdbc.update("UPDATE app.datasets_rotas SET ativo = true, publicado_em = CURRENT_TIMESTAMP WHERE id = ?", id);
        return id;
    }
}
