-- 0001_catalogos.sql
-- Cria o schema "app" e as 3 tabelas de catálogo da V1.
-- Referência: docs/06-banco-de-dados.md (seções 3, 5.2, 5.3 e 5.4)
-- Pode ser executado no SQL Editor do Supabase.

CREATE SCHEMA IF NOT EXISTS app;

-- 5.2 Perfis de mobilidade
CREATE TABLE IF NOT EXISTS app.perfis_mobilidade (
    id         bigserial PRIMARY KEY,
    uuid       uuid      NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    codigo     text      NOT NULL UNIQUE CHECK (btrim(codigo) <> ''),
    nome       text      NOT NULL CHECK (btrim(nome) <> ''),
    descricao  text,
    ativo      boolean   NOT NULL DEFAULT true
);

-- 5.3 Tipos de barreira
CREATE TABLE IF NOT EXISTS app.tipos_barreira (
    id         bigserial PRIMARY KEY,
    uuid       uuid      NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    codigo     text      NOT NULL UNIQUE CHECK (btrim(codigo) <> ''),
    nome       text      NOT NULL CHECK (btrim(nome) <> ''),
    descricao  text,
    ativo      boolean   NOT NULL DEFAULT true
);

-- 5.4 Classificação de cada barreira para cada perfil
CREATE TABLE IF NOT EXISTS app.classificacoes_barreira (
    id                    bigserial PRIMARY KEY,
    uuid                  uuid   NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    perfil_mobilidade_id  bigint NOT NULL REFERENCES app.perfis_mobilidade(id) ON DELETE RESTRICT,
    tipo_barreira_id      bigint NOT NULL REFERENCES app.tipos_barreira(id)    ON DELETE RESTRICT,
    classificacao         text   NOT NULL CHECK (classificacao IN ('impeditiva', 'atencao', 'irrelevante')),
    UNIQUE (perfil_mobilidade_id, tipo_barreira_id)
);

-- Índice das FKs não coberto por PK/UNIQUE (seção 10 do doc)
CREATE INDEX IF NOT EXISTS idx_classificacoes_tipo_barreira
    ON app.classificacoes_barreira (tipo_barreira_id);
