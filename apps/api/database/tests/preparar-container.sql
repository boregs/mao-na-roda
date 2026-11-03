\set ON_ERROR_STOP on
-- Exclusivamente para o banco novo e descartável do Compose/CI.
DO $$
BEGIN
    IF current_database() <> 'maonaroda_test'
        OR EXISTS (SELECT 1 FROM pg_namespace WHERE nspname IN ('app', 'auth')) THEN
        RAISE EXCEPTION 'Preparação exige maonaroda_test sem schemas app/auth';
    END IF;
END;
$$;
DROP EXTENSION IF EXISTS postgis_tiger_geocoder;
DROP EXTENSION IF EXISTS postgis_topology;
DROP EXTENSION IF EXISTS postgis_raster;
DROP EXTENSION IF EXISTS postgis;
\ir bootstrap-local.sql
