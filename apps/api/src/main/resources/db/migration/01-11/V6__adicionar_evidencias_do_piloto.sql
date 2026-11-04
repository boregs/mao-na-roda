-- Sem dados fictícios de acessibilidade: a carga revisada é publicada por operador.
CREATE TABLE app.datasets_rotas (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    versao text NOT NULL UNIQUE,
    versao_matriz text NOT NULL CHECK (versao_matriz = 'RN-v1'),
    publicado_em timestamptz,
    ativo boolean NOT NULL DEFAULT false,
    CHECK (NOT ativo OR publicado_em IS NOT NULL)
);
CREATE UNIQUE INDEX dataset_rota_ativo_unico ON app.datasets_rotas (ativo) WHERE ativo;

-- A assinatura identifica a geometria exata e orientada devolvida pelo provedor.
-- O vínculo só é publicado após revisão de calçada, nível, travessia e entrada.
CREATE TABLE app.trechos_pedestres (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id uuid NOT NULL REFERENCES app.datasets_rotas(id),
    assinatura_rota char(64) NOT NULL CHECK (assinatura_rota ~ '^[0-9a-f]{64}$'),
    inicio double precision NOT NULL CHECK (inicio >= 0 AND inicio < 1),
    fim double precision NOT NULL CHECK (fim > 0 AND fim <= 1 AND fim > inicio),
    geometria extensions.geometry(LineString, 4326) NOT NULL,
    lado text NOT NULL,
    nivel integer NOT NULL,
    travessia boolean NOT NULL,
    entrada boolean NOT NULL,
    contexto_revisado_em timestamptz NOT NULL,
    CHECK (extensions.ST_IsValid(geometria) AND NOT extensions.ST_IsEmpty(geometria))
);
CREATE INDEX trechos_pedestres_rota_idx ON app.trechos_pedestres (dataset_id, assinatura_rota);
CREATE INDEX trechos_pedestres_geom_idx ON app.trechos_pedestres USING gist (geometria);

CREATE TABLE app.evidencias_trecho (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    trecho_id uuid NOT NULL REFERENCES app.trechos_pedestres(id),
    tipo_barreira_id bigint NOT NULL REFERENCES app.tipos_barreira(id),
    condicao text NOT NULL CHECK (condicao IN ('presente', 'ausente', 'desconhecida')),
    fonte text NOT NULL CHECK (fonte IN ('openstreetmap', 'dados_publicos')),
    id_fonte text NOT NULL,
    versao_fonte text NOT NULL,
    lote text NOT NULL,
    data_original timestamptz,
    verificado_em timestamptz,
    motivo_sem_verificacao text,
    UNIQUE (trecho_id, tipo_barreira_id),
    CHECK ((verificado_em IS NOT NULL AND isfinite(verificado_em))
        OR (verificado_em IS NULL AND motivo_sem_verificacao IS NOT NULL
            AND length(trim(motivo_sem_verificacao)) > 0))
);
GRANT SELECT ON app.datasets_rotas, app.trechos_pedestres, app.evidencias_trecho TO maonaroda_api;
COMMENT ON TABLE app.trechos_pedestres IS
    'Piloto conservador: vínculo manual com geometria exata; nunca publicar só por proximidade.';
