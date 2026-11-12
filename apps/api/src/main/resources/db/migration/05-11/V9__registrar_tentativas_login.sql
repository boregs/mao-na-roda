-- RNF18: falhas de login por e-mail. Guarda apenas o SHA-256 do e-mail normalizado,
-- sem duplicar o e-mail do Auth. A linha é removida após um login bem-sucedido.
CREATE TABLE app.tentativas_login (
    email_hash text PRIMARY KEY CHECK (email_hash ~ '^[0-9a-f]{64}$'),
    tentativas integer NOT NULL CHECK (tentativas >= 0),
    janela_inicio timestamptz NOT NULL,
    bloqueado_ate timestamptz
);

COMMENT ON TABLE app.tentativas_login IS
    'Janela de 15 minutos de tentativas de login por e-mail (RNF18). Sem dados pessoais legíveis.';

REVOKE ALL ON app.tentativas_login FROM PUBLIC, anon, authenticated, service_role;
GRANT SELECT, INSERT, UPDATE, DELETE ON app.tentativas_login TO maonaroda_api;

ALTER TABLE app.tentativas_login ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS maonaroda_api_tentativas ON app.tentativas_login;
CREATE POLICY maonaroda_api_tentativas ON app.tentativas_login
    FOR ALL TO maonaroda_api USING (true) WITH CHECK (true);
