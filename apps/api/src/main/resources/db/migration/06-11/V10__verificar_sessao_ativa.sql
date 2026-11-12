-- RNF16: a API recusa tokens cuja sessão do Supabase Auth terminou (logout, exclusão
-- da conta ou limite de sessão). A função lê auth.sessions com os privilégios do dono,
-- sem conceder à API leitura direta do schema auth.
CREATE OR REPLACE FUNCTION app.sessao_ativa(sessao uuid, usuario uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM auth.sessions s
        WHERE s.id = sessao
            AND s.user_id = usuario
            AND (s.not_after IS NULL OR s.not_after > now())
    );
$$;

REVOKE ALL ON FUNCTION app.sessao_ativa(uuid, uuid) FROM PUBLIC, anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION app.sessao_ativa(uuid, uuid) TO maonaroda_api;
