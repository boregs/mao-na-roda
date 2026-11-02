\set ON_ERROR_STOP on

-- SOMENTE em PostgreSQL local descartável, antes do Flyway.
-- Fixture mínima do contrato externo; não implementa Supabase Auth/Storage.
-- CREATE sem IF NOT EXISTS impede execução sobre um schema auth já existente.
CREATE SCHEMA auth;
CREATE TABLE auth.users (id uuid PRIMARY KEY);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        CREATE ROLE anon NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        CREATE ROLE authenticated NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
        CREATE ROLE service_role NOLOGIN BYPASSRLS;
    END IF;
END;
$$;
