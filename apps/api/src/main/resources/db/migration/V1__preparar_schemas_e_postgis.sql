-- Supabase gerencia auth e storage. Esta migration nunca cria suas tabelas.
DO $$
BEGIN
    IF to_regclass('auth.users') IS NULL THEN
        RAISE EXCEPTION 'Supabase Auth ausente: auth.users deve existir antes das migrations';
    END IF;
END;
$$;

CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA extensions;

-- IF NOT EXISTS não garante que uma extensão existente esteja no schema esperado.
-- PostGIS não é relocável; falhar sem mover/remover objetos de outro ambiente.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_extension e
        JOIN pg_namespace n ON n.oid = e.extnamespace
        WHERE e.extname = 'postgis' AND n.nspname = 'extensions'
    ) THEN
        RAISE EXCEPTION 'PostGIS deve estar em extensions; veja database/README.md';
    END IF;
END;
$$;

CREATE SCHEMA app;
REVOKE ALL ON SCHEMA app FROM PUBLIC, anon, authenticated, service_role;

ALTER DEFAULT PRIVILEGES IN SCHEMA app
    REVOKE ALL ON TABLES FROM PUBLIC, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA app
    REVOKE ALL ON SEQUENCES FROM PUBLIC, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA app
    REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC, anon, authenticated, service_role;

COMMENT ON SCHEMA app IS
    'Domínio do Mão na Roda, acessado por JDBC. Manter fora dos schemas expostos pela Data API.';
