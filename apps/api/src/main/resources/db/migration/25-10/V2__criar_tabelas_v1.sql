CREATE TABLE app.perfis_mobilidade (
    id bigserial PRIMARY KEY,
    uuid uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    codigo text NOT NULL UNIQUE CHECK (codigo ~ '^[a-z][a-z0-9_]*$'),
    nome text NOT NULL CHECK (nome ~ '[^[:space:]]'),
    descricao text,
    ativo boolean NOT NULL DEFAULT true
);

CREATE TABLE app.tipos_barreira (
    id bigserial PRIMARY KEY,
    uuid uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    codigo text NOT NULL UNIQUE CHECK (codigo ~ '^[a-z][a-z0-9_]*$'),
    nome text NOT NULL CHECK (nome ~ '[^[:space:]]'),
    descricao text,
    ativo boolean NOT NULL DEFAULT true
);

CREATE TABLE app.classificacoes_barreira (
    id bigserial PRIMARY KEY,
    uuid uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    perfil_mobilidade_id bigint NOT NULL REFERENCES app.perfis_mobilidade(id) ON DELETE RESTRICT,
    tipo_barreira_id bigint NOT NULL REFERENCES app.tipos_barreira(id) ON DELETE RESTRICT,
    classificacao text NOT NULL CHECK (classificacao IN ('impeditiva', 'atencao', 'irrelevante')),
    CONSTRAINT classificacoes_barreira_perfil_tipo_key UNIQUE (perfil_mobilidade_id, tipo_barreira_id)
);

CREATE TABLE app.usuarios (
    id bigserial PRIMARY KEY,
    uuid uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    auth_user_id uuid NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    nome text NOT NULL CHECK (nome ~ '[^[:space:]]'),
    sobrenome text NOT NULL CHECK (sobrenome ~ '[^[:space:]]'),
    perfil_mobilidade_id bigint REFERENCES app.perfis_mobilidade(id) ON DELETE RESTRICT,
    estado_conta text NOT NULL DEFAULT 'ativa' CHECK (estado_conta IN ('ativa', 'exclusao_em_andamento')),
    criado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE app.usuarios IS
    'Modelo lógico mantido por decisão de implementação. Criptografia de campos (RNF13) ainda pendente; não duplicar e-mail ou senha do Auth.';

CREATE TABLE app.locais (
    id bigserial PRIMARY KEY,
    uuid uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    tipo text NOT NULL CHECK (tipo IN ('trecho', 'ponto')),
    nome text,
    geometria extensions.geometry(Geometry, 4326) NOT NULL,
    provedor_referencia text,
    id_externo text,
    criado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT locais_geometria_check CHECK (
        NOT extensions.ST_IsEmpty(geometria)
        AND extensions.ST_IsValid(geometria)
        AND (
            (tipo = 'trecho' AND extensions.ST_GeometryType(geometria) = 'ST_LineString')
            OR (tipo = 'ponto' AND extensions.ST_GeometryType(geometria) = 'ST_Point')
        )
        AND extensions.ST_XMin(geometria::extensions.box3d) >= -180
        AND extensions.ST_XMax(geometria::extensions.box3d) <= 180
        AND extensions.ST_YMin(geometria::extensions.box3d) >= -90
        AND extensions.ST_YMax(geometria::extensions.box3d) <= 90
    ),
    CONSTRAINT locais_referencia_externa_check CHECK (
        (provedor_referencia IS NULL AND id_externo IS NULL)
        OR (
            provedor_referencia IS NOT NULL AND id_externo IS NOT NULL
            AND provedor_referencia ~ '[^[:space:]]' AND id_externo ~ '[^[:space:]]'
        )
    )
);

CREATE TABLE app.relatos (
    id bigserial PRIMARY KEY,
    uuid uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    autor_id bigint REFERENCES app.usuarios(id) ON DELETE SET NULL,
    descricao text NOT NULL CHECK (descricao ~ '[^[:space:]]'),
    localizacao extensions.geography(Point, 4326) NOT NULL,
    local_id bigint REFERENCES app.locais(id) ON DELETE RESTRICT,
    tipo_barreira_id bigint REFERENCES app.tipos_barreira(id) ON DELETE RESTRICT,
    estado_validacao text NOT NULL DEFAULT 'nao_validado',
    criado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmado_em timestamptz,
    autor_excluido_em timestamptz,
    descartar_em timestamptz,
    CONSTRAINT relatos_localizacao_check CHECK (
        NOT extensions.ST_IsEmpty(localizacao::extensions.geometry)
        AND extensions.ST_IsValid(localizacao::extensions.geometry)
    ),
    CONSTRAINT relatos_validacao_v1_check CHECK (
        estado_validacao = 'nao_validado' AND confirmado_em IS NULL
    ),
    CONSTRAINT relatos_autoria_retencao_check CHECK (
        (autor_id IS NOT NULL AND autor_excluido_em IS NULL AND descartar_em IS NULL)
        OR (
            autor_id IS NULL AND autor_excluido_em IS NOT NULL AND descartar_em IS NOT NULL
            AND isfinite(autor_excluido_em) AND isfinite(descartar_em)
            AND autor_excluido_em >= criado_em
            AND descartar_em >= autor_excluido_em
            AND descartar_em <= (
                (autor_excluido_em AT TIME ZONE 'UTC') + INTERVAL '1 month'
            ) AT TIME ZONE 'UTC'
        )
    )
);

CREATE TABLE app.observacoes_acessibilidade (
    id bigserial PRIMARY KEY,
    uuid uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    local_id bigint NOT NULL REFERENCES app.locais(id) ON DELETE RESTRICT,
    tipo_barreira_id bigint NOT NULL REFERENCES app.tipos_barreira(id) ON DELETE RESTRICT,
    condicao text NOT NULL CHECK (condicao IN ('presente', 'ausente', 'desconhecida')),
    origem text NOT NULL CHECK (origem IN ('dados_publicos', 'openstreetmap', 'relato_usuario')),
    referencia_fonte text,
    verificado_em timestamptz NOT NULL CHECK (isfinite(verificado_em)),
    importado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    relato_origem_id bigint REFERENCES app.relatos(id) ON DELETE RESTRICT,
    ativa boolean NOT NULL DEFAULT true,
    CONSTRAINT observacoes_origem_v1_check CHECK (
        origem IN ('dados_publicos', 'openstreetmap') AND relato_origem_id IS NULL
    )
);

COMMENT ON COLUMN app.observacoes_acessibilidade.verificado_em IS
    'Data efetiva da verificação (RN06), nunca substituída automaticamente pela data de importação.';
COMMENT ON CONSTRAINT observacoes_origem_v1_check ON app.observacoes_acessibilidade IS
    'RN09/RN12: relatos não são evidências operacionais na V1. Evoluir esta restrição junto com a validação da V2.';

CREATE TABLE app.relato_fotos (
    id bigserial PRIMARY KEY,
    uuid uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    relato_id bigint NOT NULL REFERENCES app.relatos(id) ON DELETE CASCADE,
    bucket text NOT NULL DEFAULT 'relato-fotos' CHECK (bucket = 'relato-fotos'),
    caminho_objeto text NOT NULL CHECK (caminho_objeto ~ '[^[:space:]]'),
    posicao smallint NOT NULL CHECK (posicao BETWEEN 1 AND 3),
    mime_type text NOT NULL CHECK (mime_type IN ('image/jpeg', 'image/png')),
    tamanho_bytes bigint NOT NULL CHECK (tamanho_bytes BETWEEN 1 AND 5000000),
    criado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT relato_fotos_relato_posicao_key UNIQUE (relato_id, posicao),
    CONSTRAINT relato_fotos_objeto_key UNIQUE (bucket, caminho_objeto)
);

COMMENT ON TABLE app.relato_fotos IS
    'Somente metadados validados. Remover objetos pela API do Storage antes de apagar estas referências.';

CREATE INDEX classificacoes_barreira_tipo_idx ON app.classificacoes_barreira (tipo_barreira_id);
CREATE INDEX usuarios_perfil_idx ON app.usuarios (perfil_mobilidade_id);
CREATE INDEX locais_geometria_gist_idx ON app.locais USING gist (geometria);
-- Um identificador do provedor pode corresponder a vários trechos internos.
CREATE INDEX locais_referencia_externa_idx ON app.locais (provedor_referencia, id_externo);
CREATE INDEX relatos_localizacao_gist_idx ON app.relatos USING gist (localizacao);
CREATE INDEX relatos_autor_criado_idx ON app.relatos (autor_id, criado_em);
CREATE INDEX relatos_local_idx ON app.relatos (local_id);
CREATE INDEX relatos_tipo_barreira_idx ON app.relatos (tipo_barreira_id);
CREATE INDEX relatos_descartar_idx ON app.relatos (descartar_em) WHERE descartar_em IS NOT NULL;
CREATE INDEX observacoes_local_tipo_idx ON app.observacoes_acessibilidade (local_id, tipo_barreira_id);
CREATE INDEX observacoes_tipo_barreira_idx ON app.observacoes_acessibilidade (tipo_barreira_id);
CREATE INDEX observacoes_relato_origem_idx ON app.observacoes_acessibilidade (relato_origem_id)
    WHERE relato_origem_id IS NOT NULL;
