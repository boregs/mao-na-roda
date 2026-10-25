\set ON_ERROR_STOP on

-- Executar após flyway:migrate, como administrador, no banco local descartável.
-- Os dados fictícios e as funções temporárias são revertidos ao final.
BEGIN;

CREATE FUNCTION pg_temp.esperar_erro(comando text, estado_esperado text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE comando;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = ANY (string_to_array(estado_esperado, ',')) THEN
            RETURN;
        END IF;
        RAISE;
    END;
    RAISE EXCEPTION 'Comando deveria falhar com SQLSTATE %: %', estado_esperado, comando;
END;
$$;

DO $$
<<teste>>
DECLARE
    usuario_id bigint;
    local_teste_id bigint;
    relato_id bigint;
    outro_relato_id bigint;
    barreira_id bigint;
    identidade uuid := gen_random_uuid();
    prazo timestamptz;
BEGIN
    IF (SELECT count(*) FROM app.perfis_mobilidade) <> 3
       OR (SELECT count(*) FROM app.tipos_barreira) <> 10
       OR (SELECT count(*) FROM app.classificacoes_barreira) <> 30 THEN
        RAISE EXCEPTION 'Carga inicial diferente de 3 perfis, 10 barreiras e 30 classificações';
    END IF;

    IF EXISTS (
        SELECT 1 FROM app.perfis_mobilidade p CROSS JOIN app.tipos_barreira b
        LEFT JOIN app.classificacoes_barreira c
            ON c.perfil_mobilidade_id = p.id AND c.tipo_barreira_id = b.id
        WHERE p.ativo AND b.ativo AND c.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Combinação ativa sem classificação';
    END IF;

    IF (SELECT count(*) FROM app.classificacoes_barreira WHERE classificacao = 'impeditiva') <> 9
       OR (SELECT count(*) FROM app.classificacoes_barreira WHERE classificacao = 'atencao') <> 16
       OR (SELECT count(*) FROM app.classificacoes_barreira WHERE classificacao = 'irrelevante') <> 5
       OR EXISTS (
           SELECT 1 FROM app.classificacoes_barreira c
           JOIN app.tipos_barreira b ON b.id = c.tipo_barreira_id
           WHERE b.codigo = 'ausencia_piso_tatil' AND c.classificacao <> 'irrelevante'
       )
       OR NOT EXISTS (
           SELECT 1 FROM app.classificacoes_barreira c
           JOIN app.tipos_barreira b ON b.id = c.tipo_barreira_id
           JOIN app.perfis_mobilidade p ON p.id = c.perfil_mobilidade_id
           WHERE b.codigo = 'calcada_danificada_irregular'
               AND p.codigo = 'muleta_ou_outro_auxilio' AND c.classificacao = 'impeditiva'
       ) THEN
        RAISE EXCEPTION 'Classificações incompatíveis com RN01/RN02';
    END IF;

    INSERT INTO auth.users (id) VALUES (identidade);
    INSERT INTO app.usuarios (auth_user_id, nome, sobrenome)
        VALUES (identidade, 'Pessoa', 'Teste') RETURNING id INTO usuario_id;
    SELECT id INTO barreira_id FROM app.tipos_barreira WHERE codigo = 'escada_sem_alternativa';

    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.usuarios (auth_user_id, nome, sobrenome) VALUES (%L, ''Duplicado'', ''Teste'')',
        identidade), '23505');
    PERFORM pg_temp.esperar_erro(format(
        'UPDATE app.usuarios SET nome = E'' \t\n'' WHERE id = %s', usuario_id), '23514');

    INSERT INTO app.locais (tipo, geometria, provedor_referencia, id_externo)
        VALUES ('ponto', 'SRID=4326;POINT(-46.63 -23.55)', 'openstreetmap', 'teste')
        RETURNING id INTO local_teste_id;
    -- A referência externa pode ser compartilhada por trechos internos.
    INSERT INTO app.locais (tipo, geometria, provedor_referencia, id_externo)
        VALUES ('trecho', 'SRID=4326;LINESTRING(-46.63 -23.55,-46.64 -23.56)', 'openstreetmap', 'teste');
    PERFORM pg_temp.esperar_erro(
        'INSERT INTO app.locais (tipo, geometria) VALUES (''trecho'', ''SRID=4326;POINT(0 0)'')', '23514');
    PERFORM pg_temp.esperar_erro(
        'INSERT INTO app.locais (tipo, geometria) VALUES (''ponto'', ''SRID=4326;POINT EMPTY'')', '23514');
    PERFORM pg_temp.esperar_erro(
        'INSERT INTO app.locais (tipo, geometria) VALUES (''ponto'', ''SRID=4326;POINT(181 0)'')', '23514');
    PERFORM pg_temp.esperar_erro(
        'INSERT INTO app.locais (tipo, geometria, id_externo) VALUES (''ponto'', ''SRID=4326;POINT(0 0)'', ''sem-provedor'')', '23514');

    INSERT INTO app.relatos (autor_id, descricao, localizacao, local_id)
        VALUES (usuario_id, 'Calçada bloqueada', 'SRID=4326;POINT(-46.63 -23.55)', local_teste_id)
        RETURNING id INTO relato_id;
    INSERT INTO app.relatos (autor_id, descricao, localizacao)
        VALUES (usuario_id, 'Outro problema', 'SRID=4326;POINT(-46.64 -23.56)')
        RETURNING id INTO outro_relato_id;

    PERFORM pg_temp.esperar_erro(
        'INSERT INTO app.relatos (descricao, localizacao) VALUES (''Sem autor'', ''SRID=4326;POINT(0 0)'')', '23514');
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.relatos (autor_id, descricao, localizacao) VALUES (%s, ''  '', ''SRID=4326;POINT(0 0)'')', usuario_id), '23514');
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.relatos (autor_id, descricao) VALUES (%s, ''Sem localização'')', usuario_id), '23502');
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.relatos (autor_id, descricao, localizacao) VALUES (%s, ''Vazio'', ''SRID=4326;POINT EMPTY'')', usuario_id), '23514');
    PERFORM pg_temp.esperar_erro(format(
        'UPDATE app.relatos SET estado_validacao = ''confirmado'', confirmado_em = now() WHERE id = %s', relato_id), '23514');
    PERFORM pg_temp.esperar_erro(format(
        'UPDATE app.relatos SET autor_id = NULL WHERE id = %s', relato_id), '23514');

    UPDATE app.usuarios SET estado_conta = 'exclusao_em_andamento' WHERE id = usuario_id;
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.relatos (autor_id, descricao, localizacao) VALUES (%s, ''Conta bloqueada'', ''SRID=4326;POINT(0 0)'')', usuario_id), '23514');
    UPDATE app.usuarios SET estado_conta = 'ativa' WHERE id = usuario_id;

    INSERT INTO app.observacoes_acessibilidade (local_id, tipo_barreira_id, condicao, origem, verificado_em)
        VALUES (local_teste_id, barreira_id, 'ausente', 'dados_publicos', CURRENT_TIMESTAMP - INTERVAL '2 months');
    -- Evidência antiga permanece distinguível de ausência de evidência.
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.observacoes_acessibilidade (local_id, tipo_barreira_id, condicao, origem) VALUES (%s, %s, ''presente'', ''openstreetmap'')',
        local_teste_id, barreira_id), '23502');
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.observacoes_acessibilidade (local_id, tipo_barreira_id, condicao, origem, verificado_em, relato_origem_id) VALUES (%s, %s, ''presente'', ''relato_usuario'', now(), %s)',
        local_teste_id, barreira_id, relato_id), '23514');
    -- PostgreSQL 18 distingue RESTRICT (23001) da violação de FK (23503).
    PERFORM pg_temp.esperar_erro(format('DELETE FROM app.locais WHERE id = %s', local_teste_id), '23503,23001');
    PERFORM pg_temp.esperar_erro(format('DELETE FROM app.tipos_barreira WHERE id = %s', barreira_id), '23503,23001');

    INSERT INTO app.relato_fotos (relato_id, caminho_objeto, posicao, mime_type, tamanho_bytes)
        SELECT relato_id, 'teste/' || relato_id || '/' || n || '.jpg', n, 'image/jpeg', 5000000
        FROM generate_series(1, 3) n;
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.relato_fotos (relato_id, caminho_objeto, posicao, mime_type, tamanho_bytes) VALUES (%s, ''quarta.jpg'', 4, ''image/jpeg'', 1)', relato_id), '23514');
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.relato_fotos (relato_id, caminho_objeto, posicao, mime_type, tamanho_bytes) VALUES (%s, ''duplicada.jpg'', 3, ''image/jpeg'', 1)', relato_id), '23505');
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.relato_fotos (relato_id, caminho_objeto, posicao, mime_type, tamanho_bytes) VALUES (%s, %L, 1, ''image/jpeg'', 1)',
        outro_relato_id, 'teste/' || relato_id || '/1.jpg'), '23505');
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.relato_fotos (relato_id, caminho_objeto, posicao, mime_type, tamanho_bytes) VALUES (%s, ''grande.png'', 1, ''image/png'', 5000001)', outro_relato_id), '23514');
    PERFORM pg_temp.esperar_erro(format(
        'INSERT INTO app.relato_fotos (relato_id, caminho_objeto, posicao, mime_type, tamanho_bytes) VALUES (%s, ''imagem.gif'', 1, ''image/gif'', 1)', outro_relato_id), '23514');

    DELETE FROM auth.users WHERE id = identidade;
    IF EXISTS (SELECT 1 FROM app.usuarios WHERE id = usuario_id)
       OR (SELECT count(*) FROM app.relatos WHERE id IN (relato_id, outro_relato_id)) <> 2
       OR EXISTS (SELECT 1 FROM app.relatos WHERE id IN (relato_id, outro_relato_id) AND autor_id IS NOT NULL)
       OR (SELECT count(*) FROM app.relato_fotos f WHERE f.relato_id = teste.relato_id) <> 3 THEN
        RAISE EXCEPTION 'Exclusão Auth deve remover cadastro e autoria, preservando relatos e fotos';
    END IF;
    SELECT descartar_em INTO prazo FROM app.relatos WHERE id = relato_id;
    IF prazo IS NULL OR NOT EXISTS (
        SELECT 1 FROM app.relatos WHERE id = relato_id
            AND descartar_em = ((autor_excluido_em AT TIME ZONE 'UTC') + INTERVAL '1 month') AT TIME ZONE 'UTC'
    ) THEN
        RAISE EXCEPTION 'Prazo de retenção incorreto';
    END IF;
    DELETE FROM auth.users WHERE id = identidade;
    IF (SELECT descartar_em FROM app.relatos WHERE id = relato_id) IS DISTINCT FROM prazo THEN
        RAISE EXCEPTION 'Repetição da exclusão estendeu o prazo';
    END IF;
    PERFORM pg_temp.esperar_erro(format(
        'UPDATE app.relatos SET descartar_em = autor_excluido_em + INTERVAL ''2 months'' WHERE id = %s', relato_id), '23514');

    DELETE FROM app.relatos WHERE id = relato_id;
    IF EXISTS (SELECT 1 FROM app.relato_fotos f WHERE f.relato_id = teste.relato_id) THEN
        RAISE EXCEPTION 'Descarte final não removeu metadados das fotos';
    END IF;
END;
$$;

-- Exercita os grants reais, inclusive os triggers, sem executar tudo como owner.
INSERT INTO auth.users (id) VALUES (gen_random_uuid()) RETURNING id AS identidade_jdbc
\gset
SET LOCAL ROLE maonaroda_api;
INSERT INTO app.usuarios (auth_user_id, nome, sobrenome)
    VALUES (:'identidade_jdbc', 'Conta', 'JDBC') RETURNING id AS usuario_jdbc
\gset
UPDATE app.usuarios SET nome = 'Conta atualizada' WHERE id = :usuario_jdbc;
INSERT INTO app.relatos (autor_id, descricao, localizacao)
    VALUES (:usuario_jdbc, 'Teste de privilégios', 'SRID=4326;POINT(-46.63 -23.55)')
    RETURNING id AS relato_jdbc
\gset
INSERT INTO app.relato_fotos (relato_id, caminho_objeto, posicao, mime_type, tamanho_bytes)
    VALUES (:relato_jdbc, 'teste/jdbc.jpg', 1, 'image/jpeg', 1);
RESET ROLE;

-- Simula um serviço Auth com acesso somente à identidade, sem privilégios app.
CREATE ROLE maonaroda_teste_auth NOLOGIN;
GRANT USAGE ON SCHEMA auth TO maonaroda_teste_auth;
GRANT SELECT, DELETE ON auth.users TO maonaroda_teste_auth;
SET LOCAL ROLE maonaroda_teste_auth;
DELETE FROM auth.users WHERE id = :'identidade_jdbc';
RESET ROLE;
SET LOCAL ROLE maonaroda_descarte;
DELETE FROM app.relatos WHERE id = :relato_jdbc;
RESET ROLE;

DO $$
DECLARE
    papel text;
BEGIN
    FOREACH papel IN ARRAY ARRAY['anon', 'authenticated', 'service_role'] LOOP
        IF has_schema_privilege(papel, 'app', 'USAGE')
           OR has_table_privilege(papel, 'app.usuarios', 'SELECT')
           OR has_function_privilege(papel, 'app.remover_autoria_usuario()', 'EXECUTE') THEN
            RAISE EXCEPTION 'Acesso indevido ao domínio pelo papel %', papel;
        END IF;
    END LOOP;
    IF NOT has_table_privilege('maonaroda_api', 'app.relatos', 'INSERT')
       OR NOT has_sequence_privilege('maonaroda_api', 'app.relatos_id_seq', 'USAGE')
       OR has_table_privilege('maonaroda_api', 'app.relatos', 'DELETE')
       OR has_table_privilege('maonaroda_api', 'app.tipos_barreira', 'UPDATE')
       OR NOT has_table_privilege('maonaroda_descarte', 'app.relatos', 'DELETE')
       OR has_table_privilege('maonaroda_descarte', 'app.usuarios', 'SELECT')
       OR has_schema_privilege('maonaroda_api', 'app', 'CREATE') THEN
        RAISE EXCEPTION 'Privilégios incompatíveis com os papéis JDBC';
    END IF;
END;
$$;

ROLLBACK;
\echo Integridade da matriz, geometria, autoria, fotos, retenção e permissões: OK
