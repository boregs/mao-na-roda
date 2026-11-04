-- Papéis de grupo: provisionar logins e senhas fora do Git, por ambiente.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'maonaroda_api') THEN
        CREATE ROLE maonaroda_api NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'maonaroda_descarte') THEN
        CREATE ROLE maonaroda_descarte NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
    END IF;
END;
$$;

-- Revogação explícita também cobre grants herdados dos defaults do projeto Supabase.
REVOKE ALL ON SCHEMA app FROM PUBLIC, anon, authenticated, service_role;
REVOKE ALL ON ALL TABLES IN SCHEMA app FROM PUBLIC, anon, authenticated, service_role;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA app FROM PUBLIC, anon, authenticated, service_role;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA app FROM PUBLIC, anon, authenticated, service_role;

GRANT USAGE ON SCHEMA app, extensions TO maonaroda_api, maonaroda_descarte;

GRANT SELECT ON
    app.perfis_mobilidade, app.tipos_barreira, app.classificacoes_barreira,
    app.usuarios, app.locais, app.observacoes_acessibilidade, app.relatos, app.relato_fotos
TO maonaroda_api;
GRANT INSERT ON app.usuarios, app.relatos, app.relato_fotos TO maonaroda_api;
GRANT UPDATE (nome, sobrenome, perfil_mobilidade_id, estado_conta)
    ON app.usuarios TO maonaroda_api;
GRANT USAGE ON SEQUENCE app.usuarios_id_seq, app.relatos_id_seq, app.relato_fotos_id_seq
    TO maonaroda_api;

-- Apenas o worker que removeu os objetos Storage pode concluir o descarte SQL.
GRANT SELECT, DELETE ON app.relatos, app.relato_fotos TO maonaroda_descarte;

COMMENT ON ROLE maonaroda_api IS
    'Grupo da conexão JDBC da API. Autorização por usuário deve ser feita no servidor.';
COMMENT ON ROLE maonaroda_descarte IS
    'Grupo do worker de descarte. Remover arquivos pelo Storage antes dos registros SQL.';
