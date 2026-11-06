-- Cadastro próprio criado na mesma transação da conta Supabase Auth (UC06).
-- Se esta inserção falhar, o Auth desfaz a criação da conta: não há conta sem cadastro.
-- Contas criadas fora da API (painel do Supabase, fixtures) não enviam nome/sobrenome
-- e continuam sem cadastro próprio, como antes desta migration.
CREATE OR REPLACE FUNCTION app.criar_usuario_da_identidade()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    -- to_jsonb evita depender da coluna na fixture local mínima de auth.users.
    dados jsonb := to_jsonb(NEW) -> 'raw_user_meta_data';
    nome text := btrim(dados ->> 'nome');
    sobrenome text := btrim(dados ->> 'sobrenome');
BEGIN
    IF nome IS NULL OR sobrenome IS NULL
        OR nome !~ '[^[:space:]]' OR sobrenome !~ '[^[:space:]]' THEN
        RETURN NEW;
    END IF;

    INSERT INTO app.usuarios (auth_user_id, nome, sobrenome)
    VALUES (NEW.id, left(nome, 100), left(sobrenome, 100))
    ON CONFLICT (auth_user_id) DO NOTHING;
    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION app.criar_usuario_da_identidade()
    FROM PUBLIC, anon, authenticated, service_role;

DROP TRIGGER IF EXISTS maonaroda_criar_usuario ON auth.users;
CREATE TRIGGER maonaroda_criar_usuario
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION app.criar_usuario_da_identidade();
