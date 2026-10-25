CREATE FUNCTION app.atualizar_timestamp()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
BEGIN
    NEW.atualizado_em := statement_timestamp();
    RETURN NEW;
END;
$$;

CREATE TRIGGER usuarios_atualizar_timestamp
BEFORE UPDATE ON app.usuarios
FOR EACH ROW EXECUTE FUNCTION app.atualizar_timestamp();

CREATE TRIGGER locais_atualizar_timestamp
BEFORE UPDATE ON app.locais
FOR EACH ROW EXECUTE FUNCTION app.atualizar_timestamp();

CREATE FUNCTION app.exigir_autor_ativo()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    estado text;
BEGIN
    -- Serializa o envio com a mudança de estado/exclusão da conta.
    -- A API ainda deve resolver o autor pela identidade Auth e aplicar a cota RNF21.
    SELECT estado_conta INTO estado
    FROM app.usuarios
    WHERE id = NEW.autor_id
    FOR UPDATE;

    IF estado IS DISTINCT FROM 'ativa' THEN
        RAISE EXCEPTION 'Um novo relato exige autor com conta ativa'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER relatos_exigir_autor_ativo
BEFORE INSERT ON app.relatos
FOR EACH ROW EXECUTE FUNCTION app.exigir_autor_ativo();

CREATE FUNCTION app.remover_autoria_usuario()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog
AS $$
DECLARE
    marco timestamptz := statement_timestamp();
BEGIN
    -- Executa também quando o Supabase Auth provoca ON DELETE CASCADE em usuarios.
    -- Preparar a retenção ANTES do SET NULL da FK evita violar o CHECK dos relatos.
    -- Relatos já desvinculados não são alterados: uma repetição não estende o prazo.
    UPDATE app.relatos
    SET autor_id = NULL,
        autor_excluido_em = marco,
        descartar_em = ((marco AT TIME ZONE 'UTC') + INTERVAL '1 month') AT TIME ZONE 'UTC'
    WHERE autor_id = OLD.id;
    RETURN OLD;
END;
$$;

CREATE TRIGGER usuarios_remover_autoria
BEFORE DELETE ON app.usuarios
FOR EACH ROW EXECUTE FUNCTION app.remover_autoria_usuario();

REVOKE ALL ON FUNCTION app.atualizar_timestamp() FROM PUBLIC, anon, authenticated, service_role;
REVOKE ALL ON FUNCTION app.exigir_autor_ativo() FROM PUBLIC, anon, authenticated, service_role;
REVOKE ALL ON FUNCTION app.remover_autoria_usuario() FROM PUBLIC, anon, authenticated, service_role;
